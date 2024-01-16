package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isActiveInPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.isActivitiesRolledOutAt
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReceivedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Handler is responsible for un-suspending any auto-suspended allocations and suspended attendance records matching
 * the prison and prisoner for the particular offender received domain event. Note is will not unsuspend manually
 * suspended allocations.
 *
 * Will also raise allocation and attendance amended events for all allocation and attendance records updated as a
 * result.
 */
@Component
@Transactional
class OffenderReceivedEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val allocationRepository: AllocationRepository,
  private val prisonerSearchApiApplicationClient: PrisonerSearchApiApplicationClient,
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
) : EventHandler<OffenderReceivedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: OffenderReceivedEvent): Outcome {
    log.debug("Handling offender received event {}", event)

    if (rolloutPrisonRepository.isActivitiesRolledOutAt(event.prisonCode())) {
      prisonerSearchApiApplicationClient.findByPrisonerNumber(event.prisonerNumber())?.let { prisoner ->
        if (prisoner.isActiveInPrison(event.prisonCode())) {
          transactionHandler.newSpringTransaction {
            allocationRepository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
              .resetSuspendedAllocations(event)
              .resetFutureSuspendedAttendances(event)
          }.let { (activeAllocations, activeAttendances) ->
            activeAllocations.forEach {
              outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, it.allocationId)
            }.also { log.info("Sending allocation amended events.") }
            activeAttendances.forEach {
              outboundEventsService.send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, it.attendanceId)
            }.also { log.info("Sending attendance amended events.") }
          }
        } else {
          log.info("Prisoner ${event.prisonerNumber()} is not active in prison ${event.prisonCode()}")
        }

        return Outcome.success()
      }
    }

    log.debug("Ignoring received event for ${event.prisonCode()} - not rolled out.")

    return Outcome.success()
  }

  private fun List<Allocation>.resetSuspendedAllocations(event: OffenderReceivedEvent) =
    this.filter { it.status(PrisonerStatus.AUTO_SUSPENDED) }
      .onEach { it.reactivateAutoSuspensions() }
      .also {
        log.info("Reset ${this.size} suspended allocations for prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()}.")
      }

  private fun List<Allocation>.resetFutureSuspendedAttendances(event: OffenderReceivedEvent): Pair<List<Allocation>, List<Attendance>> {
    val now = LocalDateTime.now()
    val cancelledReason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.CANCELLED)

    return this to flatMap { allocation ->
      attendanceRepository.findAttendancesOnOrAfterDateForPrisoner(
        prisonCode = event.prisonCode(),
        sessionDate = LocalDate.now(),
        attendanceStatus = AttendanceStatus.COMPLETED,
        prisonerNumber = allocation.prisonerNumber,
      )
        .filter { attendance -> attendance.editable() && attendance.hasReason(AttendanceReasonEnum.SUSPENDED) }
        .filter { attendance ->
          (attendance.scheduledInstance.sessionDate == now.toLocalDate() && attendance.scheduledInstance.startTime > now.toLocalTime()) ||
            (attendance.scheduledInstance.sessionDate > now.toLocalDate())
        }
        .onEach { attendance ->
          if (attendance.scheduledInstance.cancelled) {
            // If the schedule instance was cancelled (and still is) after the initial suspension then we need to cancel instead of unsuspend
            attendance.cancel(cancelledReason).also { log.info("Cancelled attendance ${attendance.attendanceId}") }
          } else {
            attendance.unsuspend().also { log.info("Unsuspended attendance ${attendance.attendanceId}") }
          }
        }
        .also { log.info("Reset ${it.size} suspended attendances for prisoner ${allocation.prisonerNumber} allocation ID ${allocation.allocationId} at prison ${event.prisonCode()}.") }
    }
  }
}
