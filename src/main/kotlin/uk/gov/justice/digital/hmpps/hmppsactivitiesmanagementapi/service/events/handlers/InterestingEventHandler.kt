package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.CellMoveEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.IncentivesEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.NonAssociationsChangedEvent
import java.time.LocalDateTime

@Component
class InterestingEventHandler(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val allocationRepository: AllocationRepository,
  private val eventReviewRepository: EventReviewRepository,
  private val prisonApiClient: PrisonApiApplicationClient,
) : EventHandler<InboundEvent> {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun handle(event: InboundEvent): Boolean {
    log.info("Checking for interesting event: $event")

    prisonApiClient.getPrisonerDetails(event.prisonerNumber()).block()?.let { prisoner ->
      if (rolloutPrisonRepository.findByCode(prisoner.agencyId!!)?.isActivitiesRolledOut() == true) {
        if (allocationRepository.findByPrisonCodeAndPrisonerNumber(prisoner.agencyId, prisoner.offenderNo).hasActiveAllocations()) {
          val saved = eventReviewRepository.saveAndFlush(
            EventReview(
              eventTime = LocalDateTime.now(),
              eventType = this.getEventType(event),
              eventData = this.getEventMessage(event, prisoner),
              prisonCode = prisoner.agencyId,
              prisonerNumber = prisoner.offenderNo,
              bookingId = prisoner.bookingId?.toInt(),
            ),
          )
          log.info("Saved interesting event ID ${saved.eventReviewId} - ${this.getEventType(event)} - for ${prisoner.offenderNo}")
          return true
        } else {
          log.info("${prisoner.offenderNo} has no active allocations at ${prisoner.agencyId}")
        }
      } else {
        log.info("${prisoner.agencyId} is not a rolled out prison")
      }
    }
    return false
  }

  private fun getEventType(event: InboundEvent) =
    when (event) {
      is CellMoveEvent -> InboundEventType.CELL_MOVE.name
      is NonAssociationsChangedEvent -> InboundEventType.NON_ASSOCIATIONS.name
      is IncentivesEvent -> InboundEventType.INCENTIVES_UPDATED.name // TODO: Which of the incentives events?
      else -> "Unknown"
    }

  private fun getEventMessage(event: InboundEvent, prisoner: InmateDetail) =
    when (event) {
      is CellMoveEvent -> "Cell move for ${prisoner.lastName}, ${prisoner.firstName} (${prisoner.offenderNo})"
      is NonAssociationsChangedEvent -> "Non-associations for ${prisoner.lastName}, ${prisoner.firstName} (${prisoner.offenderNo})"
      is IncentivesEvent -> "Incentive level for ${prisoner.lastName}, ${prisoner.firstName} (${prisoner.offenderNo})"
      else -> "Unknown event for ${prisoner.lastName}, ${prisoner.firstName} (${prisoner.offenderNo})"
    }

  private fun List<Allocation>.hasActiveAllocations() = this.any { it.status(PrisonerStatus.ACTIVE) }
}
