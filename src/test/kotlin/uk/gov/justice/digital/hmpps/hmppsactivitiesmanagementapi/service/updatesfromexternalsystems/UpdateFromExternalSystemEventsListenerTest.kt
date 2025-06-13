package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.updatesfromexternalsystems

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class UpdateFromExternalSystemEventsListenerTest {
  private val objectMapper = jacksonObjectMapper()
  private val updateFromExternalSystemListener = UpdateFromExternalSystemsEventsListener(objectMapper)

  @Test
  fun `will handle a test event passed in`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "TestEvent",
      "description" : null,
      "messageAttributes" : {},
      "who" : "automated-test-client"
    }
    """

    assertDoesNotThrow {
      updateFromExternalSystemListener.onMessage(message)
    }
  }

  @Test
  fun `will throw an an exception when an invalid event passed in`() {
    val messageId = UUID.randomUUID().toString()
    val message = """
    {
      "messageId" : "$messageId",
      "eventType" : "InvalidEventType",
      "description" : null,
      "messageAttributes" : {},
      "who" : "automated-test-client"
    }
    """

    val exception = assertThrows<Exception> {
      updateFromExternalSystemListener.onMessage(message)
    }
    assertThat(exception.message).contains("Unrecognised message type on external system event: InvalidEventType")
  }
}
