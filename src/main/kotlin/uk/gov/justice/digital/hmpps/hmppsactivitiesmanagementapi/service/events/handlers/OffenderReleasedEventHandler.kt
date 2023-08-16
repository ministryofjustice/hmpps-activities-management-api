package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isReleasedFromCustodialSentence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isReleasedFromRemand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isReleasedOnDeath
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentOccurrenceAllocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
import java.time.LocalDate
import java.time.LocalDateTime

@Component
@Transactional
class OffenderReleasedEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val allocationRepository: AllocationRepository,
  private val appointmentOccurrenceAllocationService: AppointmentOccurrenceAllocationService,
  private val prisonApiClient: PrisonApiApplicationClient,
  private val attendanceRepository: AttendanceRepository,
  private val waitingListService: WaitingListService,
) : EventHandler<OffenderReleasedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: OffenderReleasedEvent): Outcome {
    log.info("Handling offender released event $event")

    if (rolloutPrisonRepository.findByCode(event.prisonCode())?.isActivitiesRolledOut() == true) {
      return when {
        event.isTemporary() -> {
          log.info("Ignoring temporary release $event")
          Outcome.success()
        }

        event.isPermanent() -> {
          log.info("Cancelling all future appointments for prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()}")
          cancelFutureOffenderAppointments(event)

          deallocateOffenderAllocationsAndRemoveFutureAttendances(event)
          Outcome.success()
        }

        else -> {
          log.warn("Failed to handle event $event")
          Outcome.failed()
        }
      }
    } else {
      log.info("Ignoring released event for ${event.prisonCode()} - not rolled out.")
    }

    return Outcome.success()
  }

  private fun cancelFutureOffenderAppointments(event: OffenderReleasedEvent) =
    appointmentOccurrenceAllocationService.cancelFutureOffenderAppointments(
      event.prisonCode(),
      event.prisonerNumber(),
    )

  private fun deallocateOffenderAllocationsAndRemoveFutureAttendances(event: OffenderReleasedEvent) =
    prisonApiClient.getPrisonerDetails(
      prisonerNumber = event.prisonerNumber(),
      fullInfo = true,
      extraInfo = true,
    ).block()?.let { prisoner ->
      when {
        prisoner.isReleasedOnDeath() -> DeallocationReason.DIED
        prisoner.isReleasedFromRemand() -> DeallocationReason.RELEASED
        prisoner.isReleasedFromCustodialSentence() -> DeallocationReason.RELEASED
        else -> log.warn("Unable to determine release reason for prisoner ${event.prisonerNumber()}")
          .let { null }
      }
    }?.let { reason ->
      waitingListService.declinePendingOrApprovedApplicationsFor(
        event.prisonCode(),
        setOf(event.prisonerNumber()),
        "Released",
        ServiceName.SERVICE_NAME.value,
      )

      allocationRepository.findByPrisonCodeAndPrisonerNumber(event.prisonCode(), event.prisonerNumber())
        .deallocateAffectedAllocations(reason, event)
        .removeFutureAttendances(event)
        .let { true }
    } ?: log.warn("Prisoner for $event not found").let { false }

  private fun List<Allocation>.deallocateAffectedAllocations(reason: DeallocationReason, event: OffenderReleasedEvent) =
    this.filterNot { it.status(PrisonerStatus.ENDED) }
      .map { it.deallocateNowWithReason(reason) }
      .also {
        log.info("Deallocated prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()} from ${it.size} allocations.")
      }

  private fun List<Allocation>.removeFutureAttendances(event: OffenderReleasedEvent) {
    val now = LocalDateTime.now()

    forEach { allocation ->
      attendanceRepository.findAttendancesOnOrAfterDateForPrisoner(
        prisonCode = event.prisonCode(),
        sessionDate = LocalDate.now(),
        prisonerNumber = allocation.prisonerNumber,
      ).filter { attendance ->
        (attendance.scheduledInstance.sessionDate == now.toLocalDate() && attendance.scheduledInstance.startTime > now.toLocalTime()) ||
          (attendance.scheduledInstance.sessionDate > now.toLocalDate())
      }.onEach { futureAttendance ->
        futureAttendance.scheduledInstance.remove(futureAttendance)
      }
    }
  }
}
