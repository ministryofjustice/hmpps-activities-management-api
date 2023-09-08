package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isReleasedFromCustodialSentence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isReleasedFromRemand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.extensions.isReleasedOnDeath
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentOccurrenceAllocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent

@Component
@Transactional
class OffenderReleasedEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val appointmentOccurrenceAllocationService: AppointmentOccurrenceAllocationService,
  private val prisonApiClient: PrisonApiApplicationClient,
  private val allocationHandler: PrisonerAllocationHandler,
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

          getDetailsForReleasedPrisoner(event)?.getDeallocationReasonForReleasedPrisoner(event)?.let { reason ->
            allocationHandler.deallocate(event.prisonCode(), event.prisonerNumber(), reason)
          }

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

  private fun getDetailsForReleasedPrisoner(event: OffenderReleasedEvent) =
    prisonApiClient.getPrisonerDetails(
      prisonerNumber = event.prisonerNumber(),
      fullInfo = true,
      extraInfo = true,
    ).block()

  private fun InmateDetail.getDeallocationReasonForReleasedPrisoner(event: OffenderReleasedEvent) =
    when {
      isReleasedOnDeath() -> DeallocationReason.DIED
      isReleasedFromRemand() -> DeallocationReason.RELEASED
      isReleasedFromCustodialSentence() -> DeallocationReason.RELEASED
      else -> log.warn("Unable to determine release reason for prisoner ${event.prisonerNumber()}").let { null }
    }
}
