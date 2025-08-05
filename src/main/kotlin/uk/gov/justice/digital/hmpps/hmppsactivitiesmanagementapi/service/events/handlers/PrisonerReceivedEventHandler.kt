package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.isActiveInPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerReceivedHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerReceivedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService

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
class PrisonerReceivedEventHandler(
  private val rolloutPrisonService: RolloutPrisonService,
  private val prisonerSearchApiApplicationClient: PrisonerSearchApiApplicationClient,
  private val prisonerReceivedHandler: PrisonerReceivedHandler,
) : EventHandler<PrisonerReceivedEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: PrisonerReceivedEvent): Outcome {
    log.debug("PRISONER RECEIVED: handling prisoner received event {}", event)

    if (rolloutPrisonService.isActivitiesRolledOutAt(event.prisonCode())) {
      prisonerSearchApiApplicationClient.findByPrisonerNumber(event.prisonerNumber())?.let { prisoner ->
        if (prisoner.isActiveInPrison(event.prisonCode())) {
          prisonerReceivedHandler.receivePrisoner(event.prisonCode(), event.prisonerNumber())
        } else {
          log.info("PRISONER RECEIVED: prisoner ${event.prisonerNumber()} is not active in prison ${event.prisonCode()}")
        }

        return Outcome.success()
      }
    }

    log.debug("PRISONER RECEIVED: ignoring received event for ${event.prisonCode()} - not rolled out.")

    return Outcome.success()
  }
}
