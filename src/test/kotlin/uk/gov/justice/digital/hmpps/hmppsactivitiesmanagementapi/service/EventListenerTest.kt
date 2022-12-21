package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class EventListenerTest {

  private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule().apply {
    this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  private val inboundMessageService: InboundEventsService = mock()

  private val eventListener = EventListener(objectMapper, inboundMessageService)

  @Test
  fun `inbound event is passed onto the inbound service`() {
    eventListener.incoming("/messages/prisonerReleasedReasonTransferred.json".readResourceAsText())

    verify(inboundMessageService).receive(any())
  }

  private fun String.readResourceAsText() =
    EventListenerTest::class.java.getResource(this)?.readText() ?: throw AssertionError("cannot find file $this")
}
