package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
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
) : EventHandler<ActivitiesChangedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: ActivitiesChangedEvent): Outcome {
    log.info("Handling activities changed event $event")

    if (rolloutPrisonRepository.findByCode(event.prisonCode())?.isActivitiesRolledOut() == true) {
      return when (event.action()) {
        Action.SUSPEND -> suspendPrisonerAllocationsAndAttendances(event).let { Outcome.success() }
        Action.END -> deallocatePrisonerAllocations(event).let { Outcome.success() }
        else -> log.warn("Unable to process $event, unknown action").let { Outcome.failed() }
      }
    }

    return Outcome.success()
  }

  private fun suspendPrisonerAllocationsAndAttendances(event: ActivitiesChangedEvent) =
    LocalDateTime.now().let { now ->
      allocationRepository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
        .suspendPrisonersAllocations(now, event)
        .suspendPrisonersFutureAttendances(now, event)
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
      attendanceRepository.findWaitingAttendancesOnOrAfterDateForPrisoner(
        event.prisonCode(),
        dateTime.toLocalDate(),
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

  private fun deallocatePrisonerAllocations(event: ActivitiesChangedEvent) =
    allocationRepository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
      .deallocateAffectedAllocations(DeallocationReason.TEMPORARY_ABSENCE)
      .also { log.info("Deallocated prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()} from ${it.size} allocations.") }

  private fun List<Allocation>.deallocateAffectedAllocations(reason: DeallocationReason) =
    this.filterNot { it.status(PrisonerStatus.ENDED) }.onEach { it.deallocateNowWithReason(reason) }
}
