package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import software.amazon.awssdk.http.SdkHttpResponse
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import software.amazon.awssdk.services.sns.model.ValidationException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.util.concurrent.CompletableFuture

class OutboundEventsPublisherTest {
  private val mapper = jacksonObjectMapper().registerModule(JavaTimeModule())
  private lateinit var service: OutboundEventsPublisher

  private val telemetryClient: TelemetryClient = mock()

  private val telemetryAttributesCaptor = argumentCaptor<Map<String, String>>()

  private val publishRequestCaptor = argumentCaptor<PublishRequest>()

  private val hmppsQueueService = mock<HmppsQueueService>()
  private val domainEventsTopic = mock<SnsAsyncClient>()
  private val publishResult: PublishResponse = mock<PublishResponse>()
  private val featureSwitches: FeatureSwitches = mock { on { isEnabled(any<Feature>(), any()) } doReturn true }

  @BeforeEach
  fun setup() {
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    whenever(hmppsQueueService.findByTopicId("domainevents"))
      .thenReturn(HmppsTopic("domainevents", "topicARN", domainEventsTopic))

    val sdk = SdkHttpResponse.builder().statusCode(200).build()
    whenever(publishResult.sdkHttpResponse()).thenReturn(sdk)
    whenever(domainEventsTopic.publish(any<PublishRequest>()))
      .thenReturn(CompletableFuture.completedFuture(publishResult))

    service = OutboundEventsPublisher(hmppsQueueService, telemetryClient, mapper, featureSwitches)
  }

  @Test
  fun `will add event as message`() {
    val event = OutboundHMPPSDomainEvent(
      eventType = "my-event-type",
      additionalInformation = PrisonerAttendanceDeleteInformation(123, 987),
      version = "444",
      description = "A prisoner attendance has been deleted in the activities management service",
    )

    service.send(event)

    verify(domainEventsTopic).publish(publishRequestCaptor.capture())
    val request = publishRequestCaptor.firstValue

    val expectedJson = """
      {
        "eventType": "my-event-type",
        "additionalInformation": {
          "bookingId": 123,
          "scheduledInstanceId": 987
        },
        "version": "444",
        "description": "A prisoner attendance has been deleted in the activities management service"
      }
    """

    JSONAssert.assertEquals(expectedJson, request.message(), false)

    verify(telemetryClient).trackEvent(
      ArgumentMatchers.eq("my-event-type"),
      telemetryAttributesCaptor.capture(),
      ArgumentMatchers.isNull(),
    )

    assertThat(telemetryAttributesCaptor.firstValue).containsAllEntriesOf(
      mapOf(
        "eventType" to "my-event-type",
        "primaryId" to "123",
        "secondaryId" to "987",
        "version" to "444",
        "description" to "A prisoner attendance has been deleted in the activities management service",
      ),
    )
  }

  @Test
  fun `will send telemetry for bad JSON`() {
    whenever(domainEventsTopic.publish(any<PublishRequest>()))
      .thenReturn(CompletableFuture.failedFuture(ValidationException.builder().build()))

    assertDoesNotThrow {
      service.send(
        OutboundHMPPSDomainEvent(
          eventType = "my-event-type",
          additionalInformation = PrisonerAttendanceDeleteInformation(123, 987),
          version = "444",
          description = "A prisoner attendance has been deleted in the activities management service",
        ),
      )
    }

    verify(telemetryClient, never()).trackEvent(eq("my-event-type"), any(), isNull())
    verify(telemetryClient).trackEvent(eq("my-event-type_FAILED"), any(), isNull())
  }

  @Test
  fun `will throw and send telemetry if publishing fails`() {
    whenever(domainEventsTopic.publish(any<PublishRequest>()))
      .thenThrow(RuntimeException("test"))
      .thenReturn(CompletableFuture.failedFuture(RuntimeException("test")))

    assertThatThrownBy {
      service.send(
        OutboundHMPPSDomainEvent(
          eventType = "my-event-type",
          additionalInformation = PrisonerAttendanceDeleteInformation(123, 987),
          version = "444",
          description = "A prisoner attendance has been deleted in the activities management service",
        ),
      )
    }.rootCause().isInstanceOf(RuntimeException::class.java)
      .message()
      .isEqualTo("test")

    verify(telemetryClient, never()).trackEvent(eq("my-event-type"), any(), isNull())
    verify(telemetryClient).trackEvent(eq("my-event-type_FAILED"), any(), isNull())
  }

  @Test
  fun `will throw if publishing fails with an http error`() {
    val sdk = SdkHttpResponse.builder().statusCode(500).build()
    val publishResult: PublishResponse = mock<PublishResponse>()
    whenever(publishResult.sdkHttpResponse()).thenReturn(sdk)
    whenever(domainEventsTopic.publish(any<PublishRequest>()))
      .thenReturn(CompletableFuture.completedFuture(publishResult))

    assertThatThrownBy {
      service.send(
        OutboundHMPPSDomainEvent(
          eventType = "my-event-type",
          additionalInformation = PrisonerAttendanceDeleteInformation(123, 987),
          version = "444",
          description = "A prisoner attendance has been deleted in the activities management service",
        ),
      )
    }.isInstanceOf(RuntimeException::class.java)
      .message()
      .isEqualTo("Attempt to publish message my-event-type resulted in an http 500 error")

    verify(telemetryClient, never()).trackEvent(eq("my-event-type"), any(), isNull())
    verify(telemetryClient).trackEvent(eq("my-event-type_FAILED"), any(), isNull())
  }

  @Test
  fun `will not publish event if outbound events feature is disabled`() {
    whenever(featureSwitches.isEnabled(any<Feature>(), any())).thenReturn(false)

    val event = OutboundHMPPSDomainEvent(
      eventType = "my-event-type",
      additionalInformation = PrisonerAttendanceDeleteInformation(123, 987),
      version = "444",
      description = "A prisoner attendance has been deleted in the activities management service",
    )

    OutboundEventsPublisher(hmppsQueueService, telemetryClient, mapper, featureSwitches).send(event)

    verifyNoInteractions(domainEventsTopic, telemetryClient)
  }
}
