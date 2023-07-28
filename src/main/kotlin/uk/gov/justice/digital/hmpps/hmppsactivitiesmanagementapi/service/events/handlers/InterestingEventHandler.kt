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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.IncentivesDeletedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.IncentivesInsertedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.IncentivesUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.NonAssociationsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReceivedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
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

  override fun handle(event: InboundEvent): Outcome {
    log.info("Checking for interesting event: $event")

    prisonApiClient.getPrisonerDetails(event.prisonerNumber(), fullInfo = false).block()
      ?.let { prisoner ->
        if (rolloutPrisonRepository.findByCode(prisoner.agencyId!!)
          ?.isActivitiesRolledOut() == true
        ) {
          if (allocationRepository.findByPrisonCodeAndPrisonerNumber(
              prisoner.agencyId,
              prisoner.offenderNo!!,
            ).hasActiveOrPendingAllocations()
          ) {
            val saved = eventReviewRepository.saveAndFlush(
              EventReview(
                eventTime = LocalDateTime.now(),
                eventType = event.eventType(),
                eventData = this.getEventMessage(event, prisoner),
                prisonCode = prisoner.agencyId,
                prisonerNumber = prisoner.offenderNo,
                bookingId = prisoner.bookingId?.toInt(),
              ),
            )
            log.info("Saved interesting event ID ${saved.eventReviewId} - ${event.eventType()} - for ${prisoner.offenderNo}")
            return Outcome.success()
          } else {
            log.info("${prisoner.offenderNo} has no active or pending allocations at ${prisoner.agencyId}")
          }
        } else {
          log.info("${prisoner.agencyId} is not a rolled out prison")
        }
      }
    return Outcome.failed()
  }

  private fun getEventMessage(event: InboundEvent, prisoner: InmateDetail) =
    when (event) {
      is CellMoveEvent -> "Cell move for ${prisoner.lastName}, ${prisoner.firstName} (${prisoner.offenderNo})"
      is NonAssociationsChangedEvent -> "Non-associations for ${prisoner.lastName}, ${prisoner.firstName} (${prisoner.offenderNo})"
      is IncentivesInsertedEvent -> "Incentive review created for ${prisoner.lastName}, ${prisoner.firstName} (${prisoner.offenderNo})"
      is IncentivesUpdatedEvent -> "Incentive review updated for ${prisoner.lastName}, ${prisoner.firstName} (${prisoner.offenderNo})"
      is IncentivesDeletedEvent -> "Incentive review deleted for ${prisoner.lastName}, ${prisoner.firstName} (${prisoner.offenderNo})"
      is OffenderReceivedEvent -> "Prisoner received into prison ${prisoner.agencyId}, ${prisoner.lastName}, ${prisoner.firstName} (${prisoner.offenderNo})"
      is OffenderReleasedEvent -> "Prisoner released from prison ${prisoner.agencyId}, ${prisoner.lastName}, ${prisoner.firstName} (${prisoner.offenderNo})"
      else -> "Unknown event for ${prisoner.lastName}, ${prisoner.firstName} (${prisoner.offenderNo})"
    }

  private fun List<Allocation>.hasActiveOrPendingAllocations() =
    this.any { it.status(PrisonerStatus.ACTIVE, PrisonerStatus.PENDING) }
}
