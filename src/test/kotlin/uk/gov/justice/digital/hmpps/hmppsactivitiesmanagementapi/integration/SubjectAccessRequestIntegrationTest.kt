package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.containsExactly
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarWaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SubjectAccessRequestData
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.Role
import java.time.LocalDate
import java.time.LocalTime

class SubjectAccessRequestIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun `init`() {
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes(
      listOf(
        appointmentCategoryReferenceCode("EDUC", "Education"),
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return single allocation for a same day date boundary subject access request`() {
    val response = webTestClient.getSarContent("111111", LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1))

    response.content.allocations containsExactly listOf(
      SarAllocation(
        allocationId = 1,
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerStatus = "Ended",
        startDate = LocalDate.of(2020, 1, 2),
        endDate = LocalDate.of(2020, 12, 1),
        activityId = 1,
        activitySummary = "Maths Level 1",
        payBand = "Pay band 1 (lowest)",
        createdDate = LocalDate.of(2020, 1, 1),
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return two allocations for subject access request`() {
    val response = webTestClient.getSarContent("111111", LocalDate.of(2020, 1, 1), LocalDate.of(2023, 1, 1))

    response.content.allocations containsExactlyInAnyOrder listOf(
      SarAllocation(
        allocationId = 1,
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerStatus = "Ended",
        startDate = LocalDate.of(2020, 1, 2),
        endDate = LocalDate.of(2020, 12, 1),
        activityId = 1,
        activitySummary = "Maths Level 1",
        payBand = "Pay band 1 (lowest)",
        createdDate = LocalDate.of(2020, 1, 1),
      ),
      SarAllocation(
        allocationId = 2,
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerStatus = "Active",
        startDate = TimeSource.today(),
        endDate = null,
        activityId = 1,
        activitySummary = "Maths Level 1",
        payBand = "Pay band 1 (lowest)",
        createdDate = LocalDate.of(2022, 10, 10),
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return single waiting list application with a 2 day boundary for a subject access request`() {
    val response = webTestClient.getSarContent("111222", LocalDate.of(2022, 10, 9), LocalDate.of(2022, 10, 11))

    response.content.waitingListApplications containsExactly listOf(
      SarWaitingList(
        waitingListId = 2,
        prisonCode = PENTONVILLE_PRISON_CODE,
        activitySummary = "Activity Summary WL",
        applicationDate = LocalDate.of(2023, 8, 8),
        originator = "Prison Staff",
        status = "Approved",
        statusDate = null,
        comments = null,
        createdDate = LocalDate.of(2022, 10, 10),
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return single waiting list application for a same day date boundary for a subject access request`() {
    val response = webTestClient.getSarContent("111222", LocalDate.of(2022, 10, 10), LocalDate.of(2022, 10, 10))

    response.content.waitingListApplications containsExactly listOf(
      SarWaitingList(
        waitingListId = 2,
        prisonCode = PENTONVILLE_PRISON_CODE,
        activitySummary = "Activity Summary WL",
        applicationDate = LocalDate.of(2023, 8, 8),
        originator = "Prison Staff",
        status = "Approved",
        statusDate = null,
        comments = null,
        createdDate = LocalDate.of(2022, 10, 10),
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return two waiting list applications for a subject access request`() {
    val response = webTestClient.getSarContent("111222", LocalDate.of(2022, 10, 10), LocalDate.of(2022, 10, 12))

    response.content.waitingListApplications containsExactlyInAnyOrder listOf(
      SarWaitingList(
        waitingListId = 2,
        prisonCode = PENTONVILLE_PRISON_CODE,
        activitySummary = "Activity Summary WL",
        applicationDate = LocalDate.of(2023, 8, 8),
        originator = "Prison Staff",
        status = "Approved",
        statusDate = null,
        comments = null,
        createdDate = LocalDate.of(2022, 10, 10),
      ),
      SarWaitingList(
        waitingListId = 3,
        prisonCode = PENTONVILLE_PRISON_CODE,
        activitySummary = "Activity Summary WL",
        applicationDate = LocalDate.of(2023, 8, 8),
        originator = "Prison Staff",
        status = "Approved",
        statusDate = LocalDate.of(2022, 11, 12),
        comments = "added to the waiting list",
        createdDate = LocalDate.of(2022, 10, 12),
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return 3 appointments for a subject access request (Attended, Not attended and Unknown Attendance with one unknown category)`() {
    val response = webTestClient.getSarContent("111222", LocalDate.of(2022, 10, 8), LocalDate.of(2024, 10, 10))

    response.content.appointments containsExactlyInAnyOrder listOf(
      SarAppointment(
        appointmentId = 1,
        prisonCode = PENTONVILLE_PRISON_CODE,
        category = "Education",
        startDate = LocalDate.of(2022, 10, 12),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(11, 45),
        extraInformation = "Prayer session",
        attended = "Unmarked",
        createdDate = LocalDate.of(2022, 10, 11),
      ),
      SarAppointment(
        appointmentId = 2,
        prisonCode = "PVI",
        category = "Education",
        startDate = LocalDate.of(2022, 10, 13),
        startTime = LocalTime.of(14, 0),
        endTime = LocalTime.of(15, 30),
        extraInformation = null,
        attended = "Yes",
        createdDate = LocalDate.of(2022, 10, 8),
      ),
      SarAppointment(
        appointmentId = 3,
        prisonCode = PENTONVILLE_PRISON_CODE,
        category = "Unknown category",
        startDate = LocalDate.of(2022, 10, 14),
        startTime = LocalTime.of(6, 0),
        endTime = LocalTime.of(8, 30),
        extraInformation = null,
        attended = "No",
        createdDate = LocalDate.of(2022, 10, 9),
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return one appointment for a same day date boundary for a subject access request`() {
    val response = webTestClient.getSarContent("111222", LocalDate.of(2022, 10, 10), LocalDate.of(2022, 10, 12))

    response.content.appointments containsExactly listOf(
      SarAppointment(
        appointmentId = 1,
        prisonCode = PENTONVILLE_PRISON_CODE,
        category = "Education",
        startDate = LocalDate.of(2022, 10, 12),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(11, 45),
        extraInformation = "Prayer session",
        attended = "Unmarked",
        createdDate = LocalDate.of(2022, 10, 11),
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return one attendance summaries for a subject access request`() {
    val response = webTestClient.getSarContent("A4745DZ", LocalDate.of(2023, 7, 21), LocalDate.of(2023, 7, 22))

    response.content.attendanceSummary containsExactly listOf(
      SarAttendanceSummary(
        attendanceReasonCode = "Attended",
        count = 1,
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return two attendance summaries for a subject access request`() {
    val response = webTestClient.getSarContent("G9372GQ", LocalDate.of(2023, 7, 20), LocalDate.of(2023, 7, 21))

    response.content.attendanceSummary containsExactlyInAnyOrder listOf(
      SarAttendanceSummary(
        attendanceReasonCode = "Other absence reason not listed",
        count = 1,
      ),
      SarAttendanceSummary(
        attendanceReasonCode = "Prisoner's schedule shows another activity",
        count = 1,
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return one attendance summaries for a single day query for a subject access request`() {
    val response = webTestClient.getSarContent("G9372GQ", LocalDate.of(2023, 7, 21), LocalDate.of(2023, 7, 21))

    response.content.attendanceSummary containsExactlyInAnyOrder listOf(
      SarAttendanceSummary(
        attendanceReasonCode = "Other absence reason not listed",
        count = 1,
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return one of two attendance summaries (one status is WAITING) for a subject access request`() {
    val response = webTestClient.getSarContent("A4745DZ", LocalDate.of(2023, 7, 21), LocalDate.of(2023, 7, 23))

    response.content.attendanceSummary containsExactlyInAnyOrder listOf(
      SarAttendanceSummary(
        attendanceReasonCode = "Attended",
        count = 1,
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should two attendance summaries with different counts for a subject access request`() {
    val response = webTestClient.getSarContent("A4743DZ", LocalDate.of(2023, 7, 21), LocalDate.of(2024, 7, 21))

    response.content.attendanceSummary containsExactlyInAnyOrder listOf(
      SarAttendanceSummary(
        attendanceReasonCode = "Suspended",
        count = 1,
      ),
      SarAttendanceSummary(
        attendanceReasonCode = "Attended",
        count = 2,
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return one allocation, two waiting list applications and an appointment for a subject access request`() {
    val response = webTestClient.getSarContent("111222", LocalDate.of(2022, 10, 10), LocalDate.of(2022, 10, 12))

    response.content.allocations containsExactly listOf(
      SarAllocation(
        allocationId = 3,
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerStatus = "Active",
        startDate = LocalDate.of(2022, 10, 10),
        endDate = null,
        activityId = 2,
        activitySummary = "Activity Summary WL",
        payBand = "Pay band 1 (lowest)",
        createdDate = LocalDate.of(2022, 10, 10),
      ),
    )

    response.content.waitingListApplications containsExactlyInAnyOrder listOf(
      SarWaitingList(
        waitingListId = 2,
        prisonCode = PENTONVILLE_PRISON_CODE,
        activitySummary = "Activity Summary WL",
        applicationDate = LocalDate.of(2023, 8, 8),
        originator = "Prison Staff",
        status = "Approved",
        statusDate = null,
        comments = null,
        createdDate = LocalDate.of(2022, 10, 10),
      ),
      SarWaitingList(
        waitingListId = 3,
        prisonCode = PENTONVILLE_PRISON_CODE,
        activitySummary = "Activity Summary WL",
        applicationDate = LocalDate.of(2023, 8, 8),
        originator = "Prison Staff",
        status = "Approved",
        statusDate = LocalDate.of(2022, 11, 12),
        comments = "added to the waiting list",
        createdDate = LocalDate.of(2022, 10, 12),
      ),
    )

    response.content.appointments containsExactly listOf(
      SarAppointment(
        appointmentId = 1,
        prisonCode = PENTONVILLE_PRISON_CODE,
        category = "Education",
        startDate = LocalDate.of(2022, 10, 12),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(11, 45),
        extraInformation = "Prayer session",
        attended = "Unmarked",
        createdDate = LocalDate.of(2022, 10, 11),
      ),
    )
  }

  private fun WebTestClient.getSarContent(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) =
    get()
      .uri("/subject-access-request?prn=$prisonerNumber&fromDate=$fromDate&toDate=$toDate")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(Role.SUBJECT_ACCESS_REQUEST)))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SubjectAccessRequestContent::class.java)
      .returnResult().responseBody!!
}

data class SubjectAccessRequestContent(val content: SubjectAccessRequestData)
