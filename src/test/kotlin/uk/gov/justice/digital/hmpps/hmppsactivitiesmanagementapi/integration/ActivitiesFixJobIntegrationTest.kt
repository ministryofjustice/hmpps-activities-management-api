package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import java.util.*

class ActivitiesFixJobIntegrationTest : IntegrationTestBase() {

  val uuid1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
  val uuid2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
  val uuid5 = UUID.fromString("55555555-5555-5555-5555-555555555555")
  val uuid6 = UUID.fromString("66666666-6666-6666-6666-666666666666")

  @BeforeEach
  fun setUp() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers("B2345CD", "C3456DE")

    prisonApiMockServer.stubGetLocationsForAppointments(
      "RSI",
      listOf(
        appointmentLocation(123, "RSI", userDescription = "Location 123"),
      ),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes(
      listOf(
        appointmentCategoryReferenceCode("EDUC", "Education"),
      ),
    )
  }

  @Sql("classpath:test_data/seed-activities-fix-locations-job.sql")
  @Test
  fun `should update location details`() {
    nomisMappingApiMockServer.stubDpsUuidFromNomisId(1, uuid1)
    nomisMappingApiMockServer.stubDpsUuidFromNomisId(2, uuid2)
    nomisMappingApiMockServer.stubDpsUuidFromNomisIdNotFound(4)
    nomisMappingApiMockServer.stubDpsUuidFromNomisId(5, uuid5)
    nomisMappingApiMockServer.stubDpsUuidFromNomisId(6, uuid6)

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(uuid1, location(uuid1, "RSI", "L1", "Location 1"))
    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(uuid2, location(uuid2, "RSI", "L2", "Location 2"))
    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuidNotFound(uuid5)
    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(uuid6, location(uuid6, "RSI", "L6", "Location 6"))

    webTestClient.fixLocations()

    await untilAsserted {
      assertScheduleWithInternalLocation(1, 1, uuid1, "L1", "Location 1") // Updated
      assertScheduleWithoutInternalLocation(2) // Unchanged as location_id is null
      assertScheduleWithInternalLocation(3, 2, uuid2, "L2", "Location 2") // Update because other locations with id 2 were updated
      assertScheduleWithInternalLocation(4, 2, uuid2, "L2", "Location 2") // Updated
      assertScheduleWithInternalLocation(5, 3, UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"), "Old Location Code 3", "Old Location 3") // Unchanged as dps_location_id already set
      assertScheduleWithInternalLocation(6, 4, null, "Old Location Code 4", "Old Location 4") // Unchanged as mapping service returned 404
      assertScheduleWithInternalLocation(7, 5, null, "Old Location Code 5", "Old Location 5") // Unchanged as locations inside prison service returned 404
      assertScheduleWithInternalLocation(8, 6, uuid6, "L6", "Location 6") // Updated despite earlier exceptions
    }
  }

  fun assertScheduleWithoutInternalLocation(activityId: Long) {
    assertThat(webTestClient.getSchedulesOfAnActivity(activityId)!![0].internalLocation).isNull()
  }

  fun assertScheduleWithInternalLocation(activityId: Long, locationId: Int?, dpsLocationId: UUID?, locationCode: String?, locationDescription: String?) {
    webTestClient.getSchedulesOfAnActivity(activityId).apply {
      this!![0].internalLocation!!.also {
        assertThat(it.id).isEqualTo(locationId)
        assertThat(it.dpsLocationId).isEqualTo(dpsLocationId)
        assertThat(it.code).isEqualTo(locationCode)
        assertThat(it.description).isEqualTo(locationDescription)
      }
    }
  }

  fun WebTestClient.fixLocations() {
    post()
      .uri("/job/activities-fix-locations")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isAccepted
  }

  fun WebTestClient.getSchedulesOfAnActivity(id: Long) = get()
    .uri("/activities/$id/schedules")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ActivityScheduleLite::class.java)
    .returnResult().responseBody
}
