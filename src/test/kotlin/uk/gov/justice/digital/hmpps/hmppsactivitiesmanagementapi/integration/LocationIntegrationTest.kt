package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.LocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.internalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEventsSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationPrefixDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import java.time.LocalDate

class LocationIntegrationTest : IntegrationTestBase() {
  private val prisonCode = "MDI"

  private val activityLocation1 = internalLocation(1L, prisonCode = prisonCode, description = "MDI-ACT-LOC1", userDescription = "Activity Location 1")
  private val activityLocation2 = internalLocation(2L, prisonCode = prisonCode, description = "MDI-ACT-LOC2", userDescription = "Activity Location 2")
  private val appointmentLocation1 = appointmentLocation(123, prisonCode, description = "MDI-APP-LOC1", userDescription = "Appointment Location 1")
  private val socialVisitsLocation = internalLocation(locationId = 5L, description = "SOCIAL VISITS", userDescription = "Social Visits")
  private val socialVisitsLocationSummary = LocationSummary(locationId = 5L, description = "SOCIAL VISITS", userDescription = "Social Visits")

  @Test
  fun `locations by group name - defined in properties - selects relevant locations only`() {
    val prisonCode = "RNI"
    val groupName = "House block 7"

    prisonApiMockServer.stubGetLocationsForType("RNI", "CELL", "prisonapi/locations-RNI-HB7.json")
    val result = this::class.java.getResource("/__files/prisonapi/RNI_location_groups_agency_locname.json")?.readText()

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}")
          .queryParam("groupName", groupName)
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(result!!)
  }

  @Test
  fun `locations by group name - defined in prison API - selects relevant locations only`() {
    val prisonCode = "LEI"
    val groupName = "House_block_7"

    prisonApiMockServer.stubGetLocationsForType("LEI", "CELL", "prisonapi/locations-LEI-HB7.json")
    val result = this::class.java.getResource("/__files/prisonapi/LEI_location_groups_agency_locname.json")?.readText()

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}")
          .queryParam("groupName", groupName)
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(result!!)
  }

  @Test
  fun `location groups by name - agency locations not found - returns not found`() {
    val prisonCode = "not_an_agency"
    val groupName = "House block 7"

    prisonApiMockServer.stubGetLocationsForTypeNotFound(prisonCode, "CELL")

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}")
          .queryParam("groupName", groupName)
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
      .expectBody()
      .jsonPath("$.developerMessage")
      .isEqualTo("(developer message)Locations not found for agency not_an_agency with location type CELL")
  }

  @Test
  fun `location groups by name - error from prison API - server error passed to client`() {
    val prisonCode = "any_agency"
    val groupName = "any_location_type"

    prisonApiMockServer.stubGetLocationsForTypeServerError(prisonCode, "CELL")

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}")
          .queryParam("groupName", groupName)
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().is5xxServerError
      .expectBody()
      .jsonPath("$.developerMessage")
      .isEqualTo("Error")
  }

  @Test
  fun `location groups - success - none in properties so should fetch from prison API`() {
    val result = this::class.java.getResource("/__files/prisonapi/LEI_location_groups.json")?.readText()
    val prisonCode = "LEI"

    prisonApiMockServer.stubGetLocationGroups(prisonCode, "prisonapi/LEI_location_groups.json")

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}/location-groups")
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(result!!)
  }

  @Test
  fun `get location groups - success - exist in properties file`() {
    val result = this::class.java.getResource("/__files/prisonapi/location-groups-2.json")?.readText()
    val prisonCode = "MDI"

    prisonApiMockServer.stubGetLocationGroups(prisonCode, "prisonapi/location-groups-1.json")

    webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}/location-groups")
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .json(result!!)
  }

  @Test
  fun `get location groups - not found`() {
    val prisonCode = "XXX"

    prisonApiMockServer.stubGetLocationGroupsNotFound(prisonCode)

    val errorResponse = webTestClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}/location-groups")
          .build(prisonCode)
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(errorResponse!!) {
      assertThat(errorCode).isNull()
      assertThat(developerMessage).isEqualTo("(developer message)Location groups not found for prison $prisonCode")
      assertThat(moreInfo).isNull()
      assertThat(status).isEqualTo(404)
      assertThat(userMessage).isEqualTo("(user message)Location groups not found for prison $prisonCode")
    }
  }

  @Test
  fun `get location prefix by group pattern`() {
    assertThat(webTestClient.getLocationPrefix("MDI", "Houseblock 1"))
      .isEqualTo(LocationPrefixDto("MDI-1-.+"))
  }

  @Test
  fun `get location prefix by group default`() {
    assertThat(webTestClient.getLocationPrefix("LDI", "A_B"))
      .isEqualTo(LocationPrefixDto("LDI-A-B-"))
  }

  @Test
  fun `get location events summaries authorisation required`() {
    val date = LocalDate.now()
    webTestClient.get()
      .uri("/locations/prison/$prisonCode/events-summaries?date=$date")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-3.sql")
  @Sql("classpath:test_data/seed-appointment-single-id-3.sql")
  fun `get location events summaries for date with activities, appointments and visits - 200 success`() {
    val date = LocalDate.of(2022, 10, 1)

    prisonApiMockServer.stubGetEventLocationsBooked(prisonCode, date, null, listOf(socialVisitsLocationSummary))
    prisonApiMockServer.stubGetEventLocations(prisonCode, listOf(activityLocation1, activityLocation2, appointmentLocation1, socialVisitsLocation))

    webTestClient.getInternalLocationEventsSummaries(prisonCode, date) isEqualTo listOf(
      InternalLocationEventsSummary(
        activityLocation1.locationId,
        prisonCode,
        activityLocation1.description,
        activityLocation1.userDescription!!,
      ),
      InternalLocationEventsSummary(
        activityLocation2.locationId,
        prisonCode,
        activityLocation2.description,
        activityLocation2.userDescription!!,
      ),
      InternalLocationEventsSummary(
        appointmentLocation1.locationId,
        prisonCode,
        appointmentLocation1.description,
        appointmentLocation1.userDescription!!,
      ),
      InternalLocationEventsSummary(
        socialVisitsLocation.locationId,
        prisonCode,
        socialVisitsLocation.description,
        socialVisitsLocation.userDescription!!,
      ),
    )
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-3.sql")
  @Sql("classpath:test_data/seed-appointment-single-id-3.sql")
  fun `get location events summary for date and time slot with one activity only - 200 success`() {
    val date = LocalDate.of(2022, 10, 1)
    val timeSlot = TimeSlot.PM

    prisonApiMockServer.stubGetEventLocationsBooked(prisonCode, date, timeSlot, emptyList())
    prisonApiMockServer.stubGetEventLocations(prisonCode, listOf(activityLocation1, activityLocation2, appointmentLocation1, socialVisitsLocation))

    webTestClient.getInternalLocationEventsSummaries(prisonCode, date, timeSlot) isEqualTo listOf(
      InternalLocationEventsSummary(
        activityLocation2.locationId,
        prisonCode,
        activityLocation2.description,
        activityLocation2.userDescription!!,
      ),
    )
  }

  private fun WebTestClient.getLocationPrefix(prisonCode: String, groupName: String) =
    get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/locations/prison/{prisonCode}/location-prefix")
          .queryParam("groupName", groupName)
          .build(prisonCode)
      }
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isOk
      .expectBody(LocationPrefixDto::class.java)
      .returnResult().responseBody

  private fun WebTestClient.getInternalLocationEventsSummaries(
    prisonCode: String,
    date: LocalDate?,
    timeSlot: TimeSlot? = null,
  ) = get()
    .uri("/locations/prison/$prisonCode/events-summaries?date=$date" + (timeSlot?.let { "&timeSlot=$timeSlot" } ?: ""))
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(InternalLocationEventsSummary::class.java)
    .returnResult().responseBody
}
