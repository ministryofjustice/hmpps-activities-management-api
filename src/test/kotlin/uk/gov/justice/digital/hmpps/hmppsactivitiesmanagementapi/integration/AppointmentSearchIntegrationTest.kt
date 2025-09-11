package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto.UsageType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class AppointmentSearchIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var appointmentSeriesRepository: AppointmentSeriesRepository

  // Note these time slots coincide with the Moorland prison regime SQL seed data.
  private val amRange = LocalTime.of(0, 0)..LocalTime.of(12, 59)
  private val pmRange = LocalTime.of(13, 0)..LocalTime.of(17, 59)
  private val edRange = LocalTime.of(18, 0)..LocalTime.of(23, 59)

  @BeforeEach
  fun setUp() {
    val dpsLocation1 = dpsLocation(UUID.fromString("11111111-1111-1111-1111-111111111111"), MOORLAND_PRISON_CODE, localName = "Location 123")
    val dpsLocation2 = dpsLocation(UUID.fromString("22222222-2222-2222-2222-222222222222"), MOORLAND_PRISON_CODE, localName = "Location 456")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = MOORLAND_PRISON_CODE,
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation1, dpsLocation2),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation1.id, 123),
        NomisDpsLocationMapping(dpsLocation2.id, 456),
      ),
    )
  }

  @Test
  fun `search appointments authorisation required`() {
    webTestClient.post()
      .uri("/appointments/MDI/search")
      .bodyValue(AppointmentSearchRequest(startDate = LocalDate.now()))
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments in other prison`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
    )

    webTestClient.post()
      .uri("/appointments/OTH/search")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .header(CASELOAD_ID, "OTH")
      .exchange()
      .expectStatus().isBadRequest
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments that are part of a group appointment`() {
    val request = AppointmentSearchRequest(
      appointmentType = AppointmentType.GROUP,
      startDate = LocalDate.now(),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    assertThat(results.map { it.appointmentType }.distinct().single()).isEqualTo(request.appointmentType)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments starting today`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    assertThat(results.map { it.startDate }.distinct().single()).isEqualTo(request.startDate)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments starting in the AM timeslot`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      timeSlots = listOf(TimeSlot.AM),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    results.map { it.startTime }.forEach {
      assertThat(it).isBetween(LocalTime.of(0, 0), LocalTime.of(12, 59))
    }

    results.count { it.startTime in amRange }.isEqualTo(4)
    results.count { it.startTime in pmRange }.isEqualTo(0)
    results.count { it.startTime in edRange }.isEqualTo(0)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments starting in AM and PM timeslots`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      timeSlots = listOf(TimeSlot.AM, TimeSlot.PM),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    results.map { it.startTime }.forEach {
      assertThat(it).isBetween(LocalTime.of(0, 0), LocalTime.of(16, 59))
    }

    results.count { it.startTime in amRange }.isEqualTo(4)
    results.count { it.startTime in pmRange }.isEqualTo(2)
    results.count { it.startTime in edRange }.isEqualTo(0)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments in AM, PM, and ED timeslots`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      timeSlots = listOf(TimeSlot.AM, TimeSlot.PM, TimeSlot.ED),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    results.map { it.startTime }.forEach {
      assertThat(it).isBetween(LocalTime.of(0, 0), LocalTime.of(23, 59))
    }

    results.count { it.timeSlot == TimeSlot.AM }.isEqualTo(4)
    results.count { it.timeSlot == TimeSlot.PM }.isEqualTo(2)
    results.count { it.timeSlot == TimeSlot.ED }.isEqualTo(1)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments today or tomorrow`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusDays(1),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    // Check for single prison code as date range filtering uses ORs which can cause data leaks
    assertThat(results.map { it.prisonCode }.distinct()).containsOnly("MDI")

    assertThat(results.map { it.startDate }.distinct()).containsOnly(request.startDate, request.endDate)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments for today or tomorrow starting in AM and PM timeslots`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusDays(1),
      timeSlots = listOf(TimeSlot.AM, TimeSlot.PM),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    // Check for single prison code as date range filtering uses ORs which can cause data leaks
    assertThat(results.map { it.prisonCode }.distinct()).containsOnly("MDI")

    results.map { it.startTime }.forEach {
      assertThat(it).isBetween(LocalTime.of(0, 0), LocalTime.of(16, 59))
    }

    results.count { it.startTime in amRange }.isEqualTo(8)
    results.count { it.startTime in pmRange }.isEqualTo(4)
    results.count { it.startTime in edRange }.isEqualTo(0)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments today or tomorrow in AM, PM, and ED timeslots`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusDays(1),
      timeSlots = listOf(TimeSlot.AM, TimeSlot.PM, TimeSlot.ED),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    // Check for single prison code as date range filtering uses ORs which can cause data leaks
    assertThat(results.map { it.prisonCode }.distinct()).containsOnly("MDI")

    results.map { it.startTime }.forEach {
      assertThat(it).isBetween(LocalTime.of(0, 0), LocalTime.of(23, 59))
    }

    results.count { it.timeSlot == TimeSlot.AM }.isEqualTo(8)
    results.count { it.timeSlot == TimeSlot.PM }.isEqualTo(4)
    results.count { it.timeSlot == TimeSlot.ED }.isEqualTo(2)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for next weeks appointments and not find any`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now().plusWeeks(1),
      endDate = LocalDate.now().plusDays(1).plusWeeks(1),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    results.size.isEqualTo(0)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments that are part of an appointment with category OIC`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      categoryCode = "OIC",
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    assertThat(results.map { it.category.code }.distinct().single()).isEqualTo(request.categoryCode)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments with internal location id 123`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      internalLocationId = 123,
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    assertThat(results).extracting("appointmentId").containsOnly(200L, 204L, 210L, 212L)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments with dps location id`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      dpsLocationId = UUID.fromString("44444444-4444-4444-4444-444444444444"),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    assertThat(results).extracting("appointmentId").containsOnly(201L, 211L)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for in cell appointments`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      inCell = true,
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    assertThat(results.map { it.inCell }.distinct().single()).isTrue
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments created by DIFFERENT USER`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      createdBy = "DIFFERENT.USER",
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    val appointments = appointmentSeriesRepository.findAllById(results.map { it.appointmentSeriesId })

    assertThat(appointments.map { it.createdBy }.distinct().single()).isEqualTo(request.createdBy)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments for prisoner number B2345CD`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      prisonerNumbers = listOf("B2345CD"),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    results.forEach {
      assertThat(it.attendees.map { attendee -> attendee.prisonerNumber }).contains("B2345CD")
    }
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search returns edited appointments`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    assertThat(results.filter { it.isEdited }).isNotEmpty
    results.forEach { result ->
      result.createdTime isCloseTo LocalDateTime.now()

      if (result.isEdited) {
        result.updatedTime isCloseTo LocalDateTime.now()
      }
    }
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search returns cancelled appointments`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    assertThat(results.filter { it.isCancelled }).isNotEmpty
    results.forEach { result ->
      result.createdTime isCloseTo LocalDateTime.now()

      if (result.isCancelled) {
        result.cancelledTime isCloseTo LocalDateTime.now()
        result.cancelledBy isEqualTo "DIFFERENT.USER"
      }
    }
  }

  private fun WebTestClient.searchAppointments(
    prisonCode: String,
    request: AppointmentSearchRequest,
  ) = post()
    .uri("/appointments/$prisonCode/search")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .header(CASELOAD_ID, prisonCode)
    .exchange()
    .expectStatus().isAccepted
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(AppointmentSearchResult::class.java)
    .returnResult().responseBody
}
