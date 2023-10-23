package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isAtDifferentLocationTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isInactiveOut
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isRestrictedPatient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason.RELEASED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason.TEMPORARILY_RELEASED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentAttendeeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
import java.time.LocalDateTime

@Component
@Transactional
class OffenderReleasedEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val appointmentAttendeeService: AppointmentAttendeeService,
  private val prisonSearchApiClient: PrisonerSearchApiApplicationClient,
  private val allocationHandler: PrisonerAllocationHandler,
  private val allocationRepository: AllocationRepository,
) : EventHandler<OffenderReleasedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: OffenderReleasedEvent): Outcome {
    log.debug("Handling offender released event {}", event)

    if (rolloutPrisonRepository.findByCode(event.prisonCode())?.isActivitiesRolledOut() == true) {
      return when {
        event.isTemporary() -> {
          log.info("Ignoring temporary release $event")
          Outcome.success()
        }

        event.isPermanent() -> {
          log.info("Cancelling all future appointments for prisoner ${event.prisonerNumber()} at prison ${event.prisonCode()}")
          cancelFutureOffenderAppointments(event)

          if (releasedPrisonerHasAllocationsOfInterestFor(event)) {
            val releasedPrisoner = getDetailsForReleasedPrisoner(event)
            val deallocationReason = getDeallocationReasonForReleasedPrisoner(releasedPrisoner, event)
            allocationHandler.deallocate(event.prisonCode(), event.prisonerNumber(), deallocationReason)
          } else {
            log.info("No allocations of interest for prisoner ${event.prisonerNumber()}")
          }

          Outcome.success()
        }

        else -> {
          log.warn("Failed to handle event $event")
          Outcome.failed()
        }
      }
    } else {
      log.debug("Ignoring released event for ${event.prisonCode()} - not rolled out.")
    }

    return Outcome.success()
  }

  private fun releasedPrisonerHasAllocationsOfInterestFor(event: OffenderReleasedEvent) =
    allocationRepository.existAtPrisonForPrisoner(
      event.prisonCode(),
      event.prisonerNumber(),
      PrisonerStatus.allExcuding(PrisonerStatus.ENDED).toList(),
    )

  private fun cancelFutureOffenderAppointments(event: OffenderReleasedEvent) =
    appointmentAttendeeService.removePrisonerFromFutureAppointments(
      event.prisonCode(),
      event.prisonerNumber(),
      PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
      LocalDateTime.now(),
      "OFFENDER_RELEASED_EVENT",
    )

  private fun getDetailsForReleasedPrisoner(event: OffenderReleasedEvent) =
    prisonSearchApiClient.findByPrisonerNumber(prisonerNumber = event.prisonerNumber())
      ?: throw NullPointerException("Prisoner search lookup failed for prisoner ${event.prisonerNumber()}")

  private fun getDeallocationReasonForReleasedPrisoner(prisoner: Prisoner, event: OffenderReleasedEvent) =
    when {
      prisoner.isRestrictedPatient() -> RELEASED.also { log.info("Released restricted patient ${event.prisonerNumber()} from prison ${event.prisonCode()}") }
      prisoner.isInactiveOut() -> RELEASED.also { log.info("Released inactive out prisoner ${event.prisonerNumber()} from prison ${event.prisonCode()}") }
      prisoner.isAtDifferentLocationTo(event.prisonCode()) -> RELEASED.also { log.info("Released prisoner ${event.prisonerNumber()} from prison ${event.prisonCode()} now at ${prisoner.prisonId}") }
      else -> TEMPORARILY_RELEASED.also { log.info("Temporary release or transfer of prisoner ${event.prisonerNumber()} from prison ${event.prisonCode()}") }
    }
}
