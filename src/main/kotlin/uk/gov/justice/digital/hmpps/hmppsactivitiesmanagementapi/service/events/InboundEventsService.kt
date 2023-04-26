package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReceivedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReleasedEventHandler

@Service
class InboundEventsService(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val releasedEventHandler: OffenderReleasedEventHandler,
  private val receivedEventHandler: OffenderReceivedEventHandler,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun process(inboundEvent: InboundEvent) {
    if (rolloutPrisonRepository.findByCode(inboundEvent.prisonCode())?.isActivitiesRolledOut() == true) {
      when (inboundEvent) {
        is OffenderReceivedEvent -> receivedEventHandler.handle(inboundEvent)
        is OffenderReleasedEvent -> releasedEventHandler.handle(inboundEvent)
        else -> log.warn("Unsupported event ${inboundEvent.javaClass.name}")
      }

      return
    }

    log.debug("Ignoring event $inboundEvent")
  }
}
