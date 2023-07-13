package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isActiveInPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReceivedEvent
import java.time.LocalDate
import java.time.LocalDateTime

@Component
@Transactional
class OffenderReceivedEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val allocationRepository: AllocationRepository,
  private val prisonApiClient: PrisonApiApplicationClient,
  private val attendanceRepository: AttendanceRepository,
) : EventHandler<OffenderReceivedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: OffenderReceivedEvent): Outcome {
    log.info("Handling offender received event $event")

    if (rolloutPrisonRepository.prisonIsRolledOut(event.prisonCode())) {
      prisonApiClient.getPrisonerDetails(prisonerNumber = event.prisonerNumber()).block()?.let { prisoner ->
        if (prisoner.isActiveInPrison(event.prisonCode())) {
          allocationRepository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber()).let { allocations ->
            if (allocations.isNotEmpty()) {
              allocations
                .resetSuspended(event)
                .resetFutureSuspendedAttendances(event)
            } else {
              log.info("No allocations for prisoner ${event.prisonerNumber()} in prison ${event.prisonCode()}")
            }
          }
        } else {
          log.info("Prisoner is not active in prison ${event.prisonCode()}")
        }

        return Outcome.success()
      }
    }

    log.info("Ignoring received event for ${event.prisonCode()} - not rolled out.")

    return Outcome.success()
  }

  private fun RolloutPrisonRepository.prisonIsRolledOut(prisonCode: String) =
    this.findByCode(prisonCode)?.isActivitiesRolledOut() == true

  private fun List<Allocation>.resetSuspended(event: OffenderReceivedEvent) =
    this.filter { it.status(PrisonerStatus.AUTO_SUSPENDED) }
      .onEach { it.reactivateAutoSuspensions() }
      .also {
        log.info("Reset ${this.size} suspended allocations for prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()}.")
      }

  private fun List<Allocation>.resetFutureSuspendedAttendances(event: OffenderReceivedEvent) {
    val now = LocalDateTime.now()

    forEach { allocation ->
      attendanceRepository.findAttendancesOnOrAfterDateForPrisoner(
        prisonCode = event.prisonCode(),
        sessionDate = LocalDate.now(),
        attendanceStatus = AttendanceStatus.COMPLETED,
        prisonerNumber = allocation.prisonerNumber,
      )
        .filter { attendance -> attendance.editable() && attendance.attendanceReason?.code == AttendanceReasonEnum.SUSPENDED }
        .filter { attendance ->
          (attendance.scheduledInstance.sessionDate == now.toLocalDate() && attendance.scheduledInstance.startTime > now.toLocalTime()) ||
            (attendance.scheduledInstance.sessionDate > now.toLocalDate())
        }
        .onEach(Attendance::resetSuspended)
        .also { log.info("Reset ${it.size} suspended attendances for prisoner ${allocation.prisonerNumber} allocation ID ${allocation.allocationId} at prison ${event.prisonCode()}.") }
    }
  }
}
