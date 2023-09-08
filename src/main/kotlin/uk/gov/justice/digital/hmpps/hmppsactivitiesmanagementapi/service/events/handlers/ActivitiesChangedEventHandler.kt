package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isPermanentlyReleased
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isTemporarilyReleased
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.Action
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ActivitiesChangedEvent
import java.time.LocalDateTime

@Component
@Transactional
class ActivitiesChangedEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val allocationRepository: AllocationRepository,
  private val attendanceRepository: AttendanceRepository,
  private val attendanceReasonRepository: AttendanceReasonRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient,
  private val allocationHandler: PrisonerAllocationHandler,
) : EventHandler<ActivitiesChangedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: ActivitiesChangedEvent): Outcome {
    log.info("Handling activities changed event $event")

    if (rolloutPrisonRepository.findByCode(event.prisonCode())?.isActivitiesRolledOut() == true) {
      return when (event.action()) {
        Action.SUSPEND -> suspendPrisonerAllocationsAndAttendances(event)
        Action.END -> getDeallocationReasonFor(event)?.let { reason ->
          allocationHandler.deallocate(event.prisonCode(), event.prisonerNumber(), reason)

          Outcome.success()
        } ?: Outcome.failed { "Unable to determine release reason for prisoner ${event.prisonerNumber()}" }

        else -> Outcome.failed().also { log.warn("Unable to process $event, unknown action") }
      }
    }

    return Outcome.success()
  }

  private fun suspendPrisonerAllocationsAndAttendances(event: ActivitiesChangedEvent) =
    runCatching {
      LocalDateTime.now().let { now ->
        allocationRepository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
          .suspendPrisonersAllocations(now, event)
          .suspendPrisonersFutureAttendances(now, event)
      }

      Outcome.success()
    }.getOrElse {
      log.error("An error occurred whilst trying to suspend prisoner ${event.prisonerNumber()}", it)

      Outcome.failed { "An error occurred whilst trying to suspend prisoner ${event.prisonerNumber()}" }
    }

  private fun List<Allocation>.suspendPrisonersAllocations(suspendedAt: LocalDateTime, event: ActivitiesChangedEvent) =
    filter { it.status(PrisonerStatus.ACTIVE) }
      .onEach { it.autoSuspend(suspendedAt, "Temporary absence") }
      .also { log.info("Suspended ${it.size} allocations for prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()}.") }

  private fun List<Allocation>.suspendPrisonersFutureAttendances(
    dateTime: LocalDateTime,
    event: ActivitiesChangedEvent,
  ) {
    val reason = attendanceReasonRepository.findByCode(AttendanceReasonEnum.SUSPENDED)

    forEach { allocation ->
      attendanceRepository.findAttendancesOnOrAfterDateForPrisoner(
        event.prisonCode(),
        dateTime.toLocalDate(),
        AttendanceStatus.WAITING,
        allocation.prisonerNumber,
      )
        .filter { attendance ->
          attendance.editable() && (
            (attendance.scheduledInstance.sessionDate == dateTime.toLocalDate() && attendance.scheduledInstance.startTime > dateTime.toLocalTime()) ||
              (attendance.scheduledInstance.sessionDate > dateTime.toLocalDate())
            )
        }
        .onEach { attendance -> attendance.completeWithoutPayment(reason) }
        .also { log.info("Suspended ${it.size} attendances for prisoner ${allocation.prisonerNumber} allocation ID ${allocation.allocationId} at prison ${event.prisonCode()}.") }
    }
  }

  private fun getDeallocationReasonFor(event: ActivitiesChangedEvent) =
    prisonerSearchApiClient.findByPrisonerNumber(event.prisonerNumber())?.let { prisoner ->
      when {
        prisoner.isTemporarilyReleased() -> DeallocationReason.TEMPORARILY_RELEASED
        prisoner.isPermanentlyReleased() -> DeallocationReason.RELEASED
        else -> throw IllegalStateException("Unable to determine release reason for prisoner ${event.prisonerNumber()}")
      }
    }
}
