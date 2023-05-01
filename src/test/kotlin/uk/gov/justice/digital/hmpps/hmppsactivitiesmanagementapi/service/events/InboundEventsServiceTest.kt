package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.InterestingEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReceivedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReleasedEventHandler

class InboundEventsServiceTest {
  private val receivedEventHandler: OffenderReceivedEventHandler = mock()
  private val releasedEventHandler: OffenderReleasedEventHandler = mock()
  private val interestingEventHandler: InterestingEventHandler = mock()

  private val service = InboundEventsService(releasedEventHandler, receivedEventHandler, interestingEventHandler)

  @Test
  fun `inbound released event is processed by release event handler`() {
    val inboundEvent = offenderReleasedEvent(moorlandPrisonCode, "123456")
    service.process(inboundEvent)
    verify(releasedEventHandler).handle(inboundEvent)
  }

  @Test
  fun `inbound received event is processed by received event handler`() {
    val inboundEvent = offenderReceivedFromTemporaryAbsence(moorlandPrisonCode, "123456")
    service.process(inboundEvent)
    verify(receivedEventHandler).handle(inboundEvent)
  }

  @Test
  fun `inbound interesting event is processed by interesting event handler`() {
    val inboundEvent = cellMoveEvent("123456")
    service.process(inboundEvent)
    verify(interestingEventHandler).handle(inboundEvent)
  }
}
