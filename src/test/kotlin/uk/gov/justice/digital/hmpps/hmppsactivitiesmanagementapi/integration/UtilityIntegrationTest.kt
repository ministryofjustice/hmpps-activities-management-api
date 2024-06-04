package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PublishEventUtilityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerAllocatedInformation

@TestPropertySource(
  properties = [
    "feature.event.activities.prisoner.allocation-amended=true",
  ],
)
class UtilityIntegrationTest : IntegrationTestBase() {

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Test
  fun `update allocation exclusions`() {
    val response = webTestClient.publishEvents(
      PublishEventUtilityModel(
        outboundEvent = OutboundEvent.PRISONER_ALLOCATION_AMENDED,
        identifiers = listOf(1, 1, 2),
      ),
    )

    response isEqualTo "Domain event PRISONER_ALLOCATION_AMENDED published"

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      eventType isEqualTo "activities.prisoner.allocation-amended"
      additionalInformation isEqualTo PrisonerAllocatedInformation(1)
      occurredAt isCloseTo TimeSource.now()
    }

    with(eventCaptor.secondValue) {
      eventType isEqualTo "activities.prisoner.allocation-amended"
      additionalInformation isEqualTo PrisonerAllocatedInformation(2)
      occurredAt isCloseTo TimeSource.now()
    }
  }

  private fun WebTestClient.publishEvents(model: PublishEventUtilityModel) =
    post()
      .uri("/utility/publish-events")
      .bodyValue(model)
      .exchange()
      .expectStatus().isCreated
      .expectBody(String::class.java)
      .returnResult().responseBody!!
}
