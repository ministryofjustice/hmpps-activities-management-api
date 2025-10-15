package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService

abstract class ManageAllocationsBase(
  private val monitoringService: MonitoringService,
  protected val outboundEventsService: OutboundEventsService,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  protected fun sendAllocationsAmendedEvents(allocationIds: Collection<Long>) {
    log.info("Sending allocation amended events for allocation IDs ${allocationIds.joinToString(separator = ",")}.")

    allocationIds.forEach { outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, it) }
  }

  protected fun continueToRunOnFailure(block: () -> List<Long>, failure: String = ""): List<Long> = runCatching { block() }
    .onFailure {
      monitoringService.capture(failure, it)
      log.error(failure, it)
    }
    .getOrDefault(emptyList())
}
