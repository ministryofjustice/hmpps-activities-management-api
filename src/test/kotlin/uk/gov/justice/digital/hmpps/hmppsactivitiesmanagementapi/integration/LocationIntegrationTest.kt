package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.HearingSummaryResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.LocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.internalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEventsSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.whereabouts.LocationPrefixDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonApiPrisonerScheduleFixture
import java.time.LocalDate
import java.util.*

@TestPropertySource(
  properties = [
    "prison-locations.using-regex-config=RSI,BCI,CDI,EYI,FNI,HEI,MDI,NHI,WNI,IWI,LHI,RNI,WLI,WEI,WRI",
  ],
)
class LocationIntegrationTest : IntegrationTestBase() {
  private val prisonCode = "MDI"

  private val activityLocation1 = internalLocation(1L, prisonCode = prisonCode, description = "MDI-ACT-LOC1", userDescription = "Activity Location 1")
  private val activityLocation2 = internalLocation(2L, prisonCode = prisonCode, description = "MDI-ACT-LOC2", userDescription = "Activity Location 2")
  private val activityLocation3 = internalLocation(3L, prisonCode = prisonCode, description = "MDI-ACT-LOC3", userDescription = "Activity Location 3")
  private val onWingActivity = internalLocation(4L, prisonCode = prisonCode, description = "MDI-ACT-LOC4", userDescription = "Activity Location 4")
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
  @Sql("classpath:test_data/seed-activity-id-3-on-wing.sql")
  @Sql("classpath:test_data/seed-appointment-single-id-3.sql")
  fun `get location events summaries for date with activities, appointments and visits - 200 success`() {
    val date = LocalDate.of(2022, 10, 1)

    prisonApiMockServer.stubGetEventLocationsBooked(prisonCode, date, null, listOf(socialVisitsLocationSummary))

    val activityLocation1Instance = PrisonApiPrisonerScheduleFixture.visitInstance(locationId = activityLocation1.locationId, date = date)
    val activityLocation2Instance = PrisonApiPrisonerScheduleFixture.visitInstance(locationId = activityLocation2.locationId, date = date)
    val activityLocation3Instance = PrisonApiPrisonerScheduleFixture.visitInstance(locationId = activityLocation3.locationId, date = date)
    val onWingLocation4Instance = PrisonApiPrisonerScheduleFixture.visitInstance(locationId = onWingActivity.locationId, date = date)
    val appointmentLocation1Instance = PrisonApiPrisonerScheduleFixture.visitInstance(locationId = appointmentLocation1.locationId, date = date)
    val socialVisitInstance = PrisonApiPrisonerScheduleFixture.visitInstance(locationId = socialVisitsLocation.locationId, date = date)

    val dpsLocationId1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
    val dpsLocationId2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
    val dpsLocationId123 = UUID.fromString("12312312-1231-1231-1231-123123123123")
    val dpsLocationId5 = UUID.fromString("55555555-5555-5555-5555-555555555555")

    nomisMappingApiMockServer.stubMappingsFromNomisIds(
      listOf(
        NomisDpsLocationMapping(dpsLocationId1, 1),
        NomisDpsLocationMapping(dpsLocationId2, 2),
        NomisDpsLocationMapping(UUID.randomUUID(), 66876),
        NomisDpsLocationMapping(dpsLocationId123, 123),
        NomisDpsLocationMapping(dpsLocationId5, 5),
      ),
    )

    val dpsLocation1 = dpsLocation(dpsLocationId1, "MDI", "L1", "Location MDI 1")
    val dpsLocation2 = dpsLocation(dpsLocationId2, "MDI", "L2", "Location MDI 2 Updated").copy(active = false)
    val dpsLocation123 = dpsLocation(dpsLocationId123, "MDI", "L123", "Location MDI 123")
    val dpsLocation5 = dpsLocation(dpsLocationId5, "MDI", "L5", null)

    locationsInsidePrisonApiMockServer.stubNonResidentialLocations(
      prisonCode,
      setOf(dpsLocationId1, dpsLocationId2, dpsLocationId123, dpsLocationId5),
      listOf(dpsLocation1, dpsLocation2, dpsLocation123, dpsLocation5),
    )

    prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, activityLocation1.locationId, date, null, listOf(activityLocation1Instance))
    prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, activityLocation2.locationId, date, null, listOf(activityLocation2Instance))
    prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, activityLocation3.locationId, date, null, listOf(activityLocation3Instance))
    prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, onWingActivity.locationId, date, null, listOf(onWingLocation4Instance))
    prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, appointmentLocation1.locationId, date, null, listOf(appointmentLocation1Instance))
    prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, socialVisitsLocation.locationId, date, null, listOf(socialVisitInstance))

    manageAdjudicationsApiMockServer.stubHearingsForDate(
      agencyId = prisonCode,
      date = date,
      body = mapper.writeValueAsString(
        HearingSummaryResponse(hearings = emptyList()),
      ),
    )

    webTestClient.getInternalLocationEventsSummaries(prisonCode, date) isEqualTo listOf(
      InternalLocationEventsSummary(
        activityLocation1.locationId,
        dpsLocationId1,
        prisonCode,
        dpsLocation1.code,
        dpsLocation1.localName!!,
      ),
      InternalLocationEventsSummary(
        activityLocation2.locationId,
        dpsLocationId2,
        prisonCode,
        dpsLocation2.code,
        dpsLocation2.localName!!,
      ),
      InternalLocationEventsSummary(
        appointmentLocation1.locationId,
        dpsLocationId123,
        prisonCode,
        dpsLocation123.code,
        dpsLocation123.localName!!,
      ),
      InternalLocationEventsSummary(
        socialVisitsLocation.locationId,
        dpsLocationId5,
        prisonCode,
        dpsLocation5.code,
        dpsLocation5.code,
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

    manageAdjudicationsApiMockServer.stubHearingsForDate(agencyId = prisonCode, date = date, body = mapper.writeValueAsString(HearingSummaryResponse(hearings = emptyList())))

    val dpsLocationId2 = UUID.fromString("22222222-2222-2222-2222-222222222222")

    nomisMappingApiMockServer.stubMappingsFromNomisIds(
      listOf(
        NomisDpsLocationMapping(dpsLocationId2, 2),
      ),
    )

    val dpsLocation2 = dpsLocation(dpsLocationId2, "MDI", "L2", "Location MDI 2 Updated").copy(active = false)

    locationsInsidePrisonApiMockServer.stubNonResidentialLocations(
      prisonCode,
      setOf(dpsLocationId2),
      listOf(dpsLocation2),
    )

    webTestClient.getInternalLocationEventsSummaries(prisonCode, date, timeSlot) isEqualTo listOf(
      InternalLocationEventsSummary(
        activityLocation2.locationId,
        dpsLocationId2,
        prisonCode,
        dpsLocation2.code,
        dpsLocation2.localName!!,
      ),
    )
  }

  @Test
  @Sql("classpath:test_data/seed-activity-with-advance-attendances-2.sql")
  fun `get location future events summaries excluding any sessions where prisoner has advance attendance`() {
    val tomorrow = LocalDate.now().plusDays(1)

    prisonApiMockServer.stubGetEventLocationsBooked(prisonCode, tomorrow, null, emptyList())

    val dpsLocationId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    nomisMappingApiMockServer.stubMappingsFromNomisIds(
      listOf(
        NomisDpsLocationMapping(dpsLocationId, 2),
      ),
    )

    val dpsLocation = dpsLocation(dpsLocationId, "MDI", "L2", "Location MDI 2")

    locationsInsidePrisonApiMockServer.stubNonResidentialLocations(
      prisonCode,
      setOf(dpsLocationId),
      listOf(dpsLocation),
    )

    prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, 1L, tomorrow, null, emptyList())

    manageAdjudicationsApiMockServer.stubHearingsForDate(
      agencyId = prisonCode,
      date = tomorrow,
      body = mapper.writeValueAsString(
        HearingSummaryResponse(hearings = emptyList()),
      ),
    )

    // Location 2 will not be returned as only activity has one prisoner with advance attendance
    webTestClient.getInternalLocationEventsSummaries(prisonCode, tomorrow) isEqualTo listOf(
      InternalLocationEventsSummary(
        2L,
        dpsLocationId,
        prisonCode,
        dpsLocation.code,
        dpsLocation.localName!!,
      ),
    )
  }

  private fun WebTestClient.getLocationPrefix(prisonCode: String, groupName: String) = get()
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
