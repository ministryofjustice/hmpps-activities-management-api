package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto.UsageType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.read
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPayHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PublishEventUtilityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.PayHistoryMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
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

  @Test
  @Sql("classpath:test_data/seed-activity-pay.sql")
  fun `migrate activity pay rate history - with data in activity_pay table - success`() {
    val response = webTestClient.createActivityPayHistory(listOf("ROLE_NOMIS_ACTIVITIES"))
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(PayHistoryMigrateResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(payRateDataSize).isEqualTo(21)
      assertThat(payHistoryDataSize).isEqualTo(21)
      assertThat(message).isEqualTo("Activities pay rate history migration has been completed successfully for all records")
    }

    var activityPayHistoryList = webTestClient.getActivityPayHistory(1L)
    activityPayHistoryList.forEach { it.changedTime = null }
    var expectedActivityPayHistory = mapper.read<List<ActivityPayHistory>>("activity/activity-pay-history-1.json")
    assertThat(activityPayHistoryList).size().isEqualTo(10)
    assertThat(activityPayHistoryList).isEqualTo(expectedActivityPayHistory)

    activityPayHistoryList = webTestClient.getActivityPayHistory(2L)
    activityPayHistoryList.forEach { it.changedTime = null }
    expectedActivityPayHistory = mapper.read<List<ActivityPayHistory>>("activity/activity-pay-history-2.json")
    assertThat(activityPayHistoryList).size().isEqualTo(8)
    assertThat(activityPayHistoryList).isEqualTo(expectedActivityPayHistory)

    activityPayHistoryList = webTestClient.getActivityPayHistory(3L)
    activityPayHistoryList.forEach { it.changedTime = null }
    expectedActivityPayHistory = mapper.read<List<ActivityPayHistory>>("activity/activity-pay-history-3.json")
    assertThat(activityPayHistoryList).size().isEqualTo(3)
    assertThat(activityPayHistoryList).isEqualTo(expectedActivityPayHistory)
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

  private fun WebTestClient.createActivityPayHistory(roles: List<String>) = post()
    .uri("/utility/create-pay-history")
    .headers(setAuthorisation(roles = roles))
    .exchange()

  fun WebTestClient.getActivityPayHistory(id: Long) = get()
    .uri("/activities/$id/pay-history")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ActivityPayHistory::class.java)
    .returnResult().responseBody!!
}
