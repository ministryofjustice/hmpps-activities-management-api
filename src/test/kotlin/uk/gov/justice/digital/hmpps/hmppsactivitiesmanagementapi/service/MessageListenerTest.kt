package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class MessageListenerTest {

  private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule().apply {
    this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  private val inboundMessageService: InboundMessageService = mock()

  private val messageListener = MessageListener(objectMapper, inboundMessageService)

  @Test
  fun `inbound event is passed onto the inbound message service`() {
    messageListener.incoming("/messages/prisonerReleasedReasonTransferred.json".readResourceAsText())

    verify(inboundMessageService).handleEvent(any())
  }

  private fun String.readResourceAsText() =
    MessageListenerTest::class.java.getResource(this)?.readText() ?: throw AssertionError("cannot find file $this")
}
