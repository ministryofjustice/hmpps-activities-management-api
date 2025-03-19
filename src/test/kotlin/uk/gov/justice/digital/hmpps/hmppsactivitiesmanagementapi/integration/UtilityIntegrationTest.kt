package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto.UsageType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PublishEventUtilityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerAllocatedInformation
import java.util.*

@TestPropertySource(
  properties = [
    "feature.event.activities.prisoner.allocation-amended=true",
    "migrate.activities-live=LEI,RSI",
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

  @Sql("classpath:test_data/seed-activity-id-34.sql")
  @Test
  fun `returns a list of activities with invalid location`() {
    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "LEI",
      usageType = UsageType.PROGRAMMES_ACTIVITIES,
      dpsLocationIds = setOf(UUID.fromString("99999999-9999-9999-9999-999999999999")),
    )

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "RSI",
      usageType = UsageType.PROGRAMMES_ACTIVITIES,
      dpsLocationIds = setOf(UUID.fromString("11111111-1111-1111-1111-111111111111")),
    )

    val response = webTestClient.getInvalidActivityLocations()

    val expectedResult = """
      Prison Code,Activity ID,Activity Description,Internal Location ID,Internal Location Code,Internal Location Description,DPS Location ID
      RSI,3,Activity 3,2,L2,Location 2,22222222-2222-2222-2222-222222222222
      
    """.trimIndent()

    response isEqualTo expectedResult
  }

  private fun WebTestClient.publishEvents(model: PublishEventUtilityModel) = post()
    .uri("/utility/publish-events")
    .bodyValue(model)
    .exchange()
    .expectStatus().isCreated
    .expectBody(String::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.getInvalidActivityLocations() = get()
    .uri("/utility/invalid-activity-locations")
    .exchange()
    .expectStatus().isOk
    .expectBody(String::class.java)
    .returnResult().responseBody!!
}
