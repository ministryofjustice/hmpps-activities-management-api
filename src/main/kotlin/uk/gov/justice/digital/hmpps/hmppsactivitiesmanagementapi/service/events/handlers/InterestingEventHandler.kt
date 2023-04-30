package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.CellMoveEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.IncentivesEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.NonAssociationsChangedEvent

@Component
class InterestingEventHandler(
  private val prisonApiClient: PrisonApiApplicationClient,
  private val allocationRepository: AllocationRepository,
): EventHandler<InboundEvent> {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: InboundEvent): Boolean {

    when {
      event is CellMoveEvent -> {
        // Get prisoner detail
      }
      event is NonAssociationsChangedEvent -> {
        // Get prisoner detail
      }
      event is IncentivesEvent -> {
        // Get prisoner detail
      }
      else -> return false
    }

    // Call isInteresting(event)?
    // Check if they are in a rolled out prison
    // Check if they have current allocations?
    // If interesting ..save it to the event_review table
    // Else discard it

    return true
  }
}
