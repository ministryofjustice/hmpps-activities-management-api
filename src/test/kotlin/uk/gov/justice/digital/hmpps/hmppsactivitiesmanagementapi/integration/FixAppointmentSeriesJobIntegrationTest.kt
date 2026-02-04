package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import java.util.*

class FixAppointmentSeriesJobIntegrationTest : IntegrationTestBase() {

  val uuid1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
  val uuid2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
  val uuid5 = UUID.fromString("55555555-5555-5555-5555-555555555555")
  val uuid6 = UUID.fromString("66666666-6666-6666-6666-666666666666")

  @Sql("classpath:test_data/seed-fix-appointment-series-locations-job.sql")
  @Test
  fun `should update appointment series location details`() {
    nomisMappingApiMockServer.stubMappingFromNomisId(1, uuid1)
    nomisMappingApiMockServer.stubMappingFromNomisId(2, uuid2)
    nomisMappingApiMockServer.stubMappingFromNomisIdNotFound(4)
    nomisMappingApiMockServer.stubMappingFromNomisId(5, uuid5)
    nomisMappingApiMockServer.stubMappingFromNomisId(6, uuid6)

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(uuid1, dpsLocation(uuid1, "RSI", "L1", "Location 1"))
    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(uuid2, dpsLocation(uuid2, "RSI", "L2", "Location 2"))
    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuidNotFound(uuid5)
    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(uuid6, dpsLocation(uuid6, "RSI", "L6", "Location 6"))

    webTestClient.fixLocations()

    await untilAsserted {
      assertAppointmentSeries(1, 1, uuid1) // Updated
      assertAppointmentSeries(2, null, null) // Unchanged as internal_location_id is null
      assertAppointmentSeries(3, 2, uuid2) // Update because other locations with id 2 were updated
      assertAppointmentSeries(4, 2, uuid2) // Updated
      assertAppointmentSeries(5, 3, UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")) // Unchanged as dps_location_id already set
      assertAppointmentSeries(6, 4, null) // Unchanged as mapping service returned 404
      assertAppointmentSeries(7, 5, null) // Unchanged as locations inside prison service returned 404
      assertAppointmentSeries(8, 6, uuid6) // Updated despite earlier exceptions
    }
  }

  fun assertAppointmentSeries(id: Long, locationId: Long?, dpsLocationId: UUID?) {
    val appointmentSeries = webTestClient.getAppointmentSeriesById(id)!!
    with(appointmentSeries) {
      assertThat(this.internalLocationId).isEqualTo(locationId)
      assertThat(this.dpsLocationId).isEqualTo(dpsLocationId)
    }
  }

  fun WebTestClient.fixLocations() {
    post()
      .uri("/job/fix-locations")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isAccepted
  }

  fun WebTestClient.getAppointmentSeriesById(id: Long) = get()
    .uri("/appointment-series/$id")
    .headers(setAuthorisationAsClient(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(AppointmentSeries::class.java)
    .returnResult().responseBody
}
