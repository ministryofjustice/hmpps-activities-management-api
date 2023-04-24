package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode

class EventListenerTest {

  private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule().apply {
    this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  private val inboundEventsService: InboundEventsService = mock()

  private val featureSwitches: FeatureSwitches = mock()

  private val eventListener = InboundEventsListener(objectMapper, inboundEventsService, featureSwitches)

  @Test
  fun `inbound event is passed onto the inbound service`() {
    featureSwitches.stub { on { isEnabled(InboundEventType.OFFENDER_RELEASED) } doReturn true }

    val rawMessage = "/messages/prisonerReleasedReasonTransferred.json".readResourceAsText()

    eventListener.onMessage(rawMessage)

    verify(inboundEventsService).process(offenderTransferReleasedEvent(moorlandPrisonCode, "A1244AB"))
  }

  private fun String.readResourceAsText() =
    EventListenerTest::class.java.getResource(this)?.readText() ?: throw AssertionError("cannot find file $this")
}
