package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto.UsageType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import java.util.*

class AppointmentLocationIntegrationTest : IntegrationTestBase() {
  @Test
  fun `get list of appointment locations`() {
    val dpsLocation1 = dpsLocation(UUID.fromString("11111111-1111-1111-1111-111111111111"), MOORLAND_PRISON_CODE, localName = "Kitchen")
    val dpsLocation2 = dpsLocation(UUID.fromString("22222222-2222-2222-2222-222222222222"), MOORLAND_PRISON_CODE, localName = "Chapel")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = MOORLAND_PRISON_CODE,
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation1, dpsLocation2),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation1.id, 1),
        NomisDpsLocationMapping(dpsLocation2.id, 2),
      ),
    )

    assertThat(webTestClient.getAppointmentLocations()!!).containsExactlyInAnyOrder(
      AppointmentLocationSummary(id = 1, dpsLocationId = dpsLocation1.id, prisonCode = MOORLAND_PRISON_CODE, description = "Kitchen"),
      AppointmentLocationSummary(id = 2, dpsLocationId = dpsLocation2.id, prisonCode = MOORLAND_PRISON_CODE, description = "Chapel"),
    )
  }

  @Test
  fun `authorisation required`() {
    webTestClient.get()
      .uri("/appointment-locations/{prisonCode}", MOORLAND_PRISON_CODE)
      .exchange()
      .expectStatus().isUnauthorized
  }

  private fun WebTestClient.getAppointmentLocations() = get()
    .uri("/appointment-locations/{prisonCode}", MOORLAND_PRISON_CODE)
    .headers(setAuthorisationAsClient(roles = listOf(ROLE_ACTIVITY_ADMIN)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(AppointmentLocationSummary::class.java)
    .returnResult().responseBody
}
