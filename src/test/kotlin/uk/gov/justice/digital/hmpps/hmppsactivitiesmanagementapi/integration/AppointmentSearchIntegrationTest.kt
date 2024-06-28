package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import java.time.LocalDate
import java.time.LocalTime

class AppointmentSearchIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var appointmentSeriesRepository: AppointmentSeriesRepository

  // Note these time slots coincide with the Moorland prison regime SQL seed data.
  private val amRange = LocalTime.of(0, 0)..LocalTime.of(12, 59)
  private val pmRange = LocalTime.of(13, 0)..LocalTime.of(17, 59)
  private val edRange = LocalTime.of(18, 0)..LocalTime.of(23, 59)

  @Test
  fun `search appointments authorisation required`() {
    webTestClient.post()
      .uri("/appointments/MDI/search")
      .bodyValue(AppointmentSearchRequest())
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments in prison with no appointments`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusMonths(1),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "NAP",
      listOf(
        appointmentLocation(123, "NAP", userDescription = "Location 123"),
        appointmentLocation(456, "NAP", userDescription = "Location 456"),
      ),
    )

    val results = webTestClient.searchAppointments("NAP", request)!!

    assertThat(results).hasSize(0)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments in other prison`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusMonths(1),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "OTH",
      listOf(
        appointmentLocation(789, "OTH", userDescription = "Location 789"),
      ),
    )

    val results = webTestClient.searchAppointments("OTH", request)!!

    assertThat(results.map { it.prisonCode }.distinct().single()).isEqualTo("OTH")
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments that are part of a group appointment`() {
    val request = AppointmentSearchRequest(
      appointmentType = AppointmentType.GROUP,
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusMonths(1),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(123, "MDI", userDescription = "Location 123"),
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
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

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(123, "MDI", userDescription = "Location 123"),
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    assertThat(results.map { it.startDate }.distinct().single()).isEqualTo(request.startDate)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments starting within a week`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusWeeks(1),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(123, "MDI", userDescription = "Location 123"),
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    results.map { it.startDate }.forEach {
      assertThat(it).isBetween(request.startDate, request.endDate)
    }

    assertThat(results.filter { it.startDate == request.startDate }).isNotEmpty
    assertThat(results.filter { it.startDate == request.endDate }).isNotEmpty
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments starting in the AM timeslot`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusMonths(1),
      timeSlots = listOf(TimeSlot.AM),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(123, "MDI", userDescription = "Location 123"),
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    results.map { it.startTime }.forEach {
      assertThat(it).isBetween(LocalTime.of(0, 0), LocalTime.of(12, 59))
    }

    results.count { it.startTime in amRange }.isEqualTo(3)
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
      endDate = LocalDate.now().plusMonths(1),
      timeSlots = listOf(TimeSlot.AM, TimeSlot.PM),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(123, "MDI", userDescription = "Location 123"),
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    results.map { it.startTime }.forEach {
      assertThat(it).isBetween(LocalTime.of(0, 0), LocalTime.of(16, 59))
    }

    results.count { it.startTime in amRange }.isEqualTo(3)
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
      endDate = LocalDate.now().plusMonths(1),
      timeSlots = listOf(TimeSlot.AM, TimeSlot.PM, TimeSlot.ED),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(123, "MDI", userDescription = "Location 123"),
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    results.map { it.startTime }.forEach {
      assertThat(it).isBetween(LocalTime.of(0, 0), LocalTime.of(23, 59))
    }

    results.count { TimeSlot.slot(it.startTime) == TimeSlot.AM }.isEqualTo(2)
    results.count { TimeSlot.slot(it.startTime) == TimeSlot.PM }.isEqualTo(3)
    results.count { TimeSlot.slot(it.startTime) == TimeSlot.ED }.isEqualTo(1)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for appointments that are part of an appointment with category AC1`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusMonths(1),
      categoryCode = "AC1",
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(123, "MDI", userDescription = "Location 123"),
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
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
      endDate = LocalDate.now().plusMonths(1),
      internalLocationId = 123,
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(123, "MDI", userDescription = "Location 123"),
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    assertThat(results.map { it.internalLocation!!.id }.distinct().single()).isEqualTo(request.internalLocationId)
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search for in cell appointments`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusMonths(1),
      inCell = true,
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(123, "MDI", userDescription = "Location 123"),
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
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
      endDate = LocalDate.now().plusMonths(1),
      createdBy = "DIFFERENT.USER",
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(123, "MDI", userDescription = "Location 123"),
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
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
      endDate = LocalDate.now().plusMonths(1),
      prisonerNumbers = listOf("B2345CD"),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(123, "MDI", userDescription = "Location 123"),
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
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
      endDate = LocalDate.now().plusMonths(1),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(123, "MDI", userDescription = "Location 123"),
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    assertThat(results.filter { it.isEdited }).isNotEmpty
  }

  @Sql(
    "classpath:test_data/seed-appointment-search.sql",
  )
  @Test
  fun `search returns cancelled appointments`() {
    val request = AppointmentSearchRequest(
      startDate = LocalDate.now(),
      endDate = LocalDate.now().plusMonths(1),
    )

    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()
    prisonApiMockServer.stubGetLocationsForAppointments(
      "MDI",
      listOf(
        appointmentLocation(123, "MDI", userDescription = "Location 123"),
        appointmentLocation(456, "MDI", userDescription = "Location 456"),
      ),
    )

    val results = webTestClient.searchAppointments("MDI", request)!!

    assertThat(results.filter { it.isCancelled }).isNotEmpty
  }

  private fun WebTestClient.searchAppointments(
    prisonCode: String,
    request: AppointmentSearchRequest,
  ) =
    post()
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
