package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
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
import java.util.UUID

class SubjectAccessRequestIntegrationTest : IntegrationTestBase() {

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
        activityCategoryName = "Education",
        activityCategoryDescription = "Such as classes in English, maths, construction or barbering",
        attendanceRequired = true,
        paid = true,
        outsideWork = true,
        riskLevel = "low",
        organiser = "Prison staff",
        inCell = true,
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
        activityCategoryName = "Education",
        activityCategoryDescription = "Such as classes in English, maths, construction or barbering",
        attendanceRequired = true,
        paid = true,
        outsideWork = true,
        riskLevel = "low",
        organiser = "Prison staff",
        inCell = true,
      ),
      SarAllocation(
        allocationId = 2,
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerStatus = "Active",
        startDate = LocalDate.of(2022, 11, 27),
        endDate = null,
        activityId = 1,
        activitySummary = "Maths Level 1",
        payBand = "Pay band 1 (lowest)",
        createdDate = LocalDate.of(2022, 10, 10),
        activityCategoryName = "Education",
        activityCategoryDescription = "Such as classes in English, maths, construction or barbering",
        attendanceRequired = true,
        paid = true,
        outsideWork = true,
        riskLevel = "low",
        organiser = "Prison staff",
        inCell = true,
      ),
      SarAllocation(
        allocationId = 3,
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerStatus = "Active",
        startDate = LocalDate.of(2022, 11, 27),
        endDate = null,
        activityId = 2,
        activitySummary = "Activity Summary WL",
        createdDate = LocalDate.of(2022, 10, 10),
        activityCategoryName = "Industries",
        activityCategoryDescription = "Such as work like recycling, packing or assembly operated by the prison, external firms or charities",
        attendanceRequired = false,
        paid = false,
        outsideWork = false,
        riskLevel = "high",
        organiser = "A prisoner or group of prisoners",
        dpsLocationId = UUID.fromString("4475b5d5-873c-4f88-a5b7-2d20e9224a62"),
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
        originator = "Prison staff",
        status = "In progress",
        statusDate = null,
        comments = null,
        createdDate = LocalDate.of(2022, 10, 10),
        activityCategoryName = "Industries",
        activityCategoryDescription = "Such as work like recycling, packing or assembly operated by the prison, external firms or charities",
        attendanceRequired = false,
        paid = false,
        outsideWork = false,
        riskLevel = "high",
        organiser = "A prisoner or group of prisoners",
        dpsLocationId = UUID.fromString("4475b5d5-873c-4f88-a5b7-2d20e9224a62"),
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
        originator = "Prison staff",
        status = "In progress",
        statusDate = null,
        comments = null,
        createdDate = LocalDate.of(2022, 10, 10),
        activityCategoryName = "Industries",
        activityCategoryDescription = "Such as work like recycling, packing or assembly operated by the prison, external firms or charities",
        attendanceRequired = false,
        paid = false,
        outsideWork = false,
        riskLevel = "high",
        organiser = "A prisoner or group of prisoners",
        dpsLocationId = UUID.fromString("4475b5d5-873c-4f88-a5b7-2d20e9224a62"),
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
        originator = "Prison staff",
        status = "In progress",
        statusDate = null,
        comments = null,
        createdDate = LocalDate.of(2022, 10, 10),
        activityCategoryName = "Industries",
        activityCategoryDescription = "Such as work like recycling, packing or assembly operated by the prison, external firms or charities",
        attendanceRequired = false,
        paid = false,
        outsideWork = false,
        riskLevel = "high",
        organiser = "A prisoner or group of prisoners",
        dpsLocationId = UUID.fromString("4475b5d5-873c-4f88-a5b7-2d20e9224a62"),
      ),
      SarWaitingList(
        waitingListId = 3,
        prisonCode = PENTONVILLE_PRISON_CODE,
        activitySummary = "Activity Summary WL",
        applicationDate = LocalDate.of(2023, 8, 8),
        originator = "Prison staff",
        status = "Approved",
        statusDate = LocalDate.of(2022, 11, 12),
        comments = "added to the waiting list",
        createdDate = LocalDate.of(2022, 10, 12),
        declinedReason = "Activity ended",
        activityCategoryName = "Industries",
        activityCategoryDescription = "Such as work like recycling, packing or assembly operated by the prison, external firms or charities",
        attendanceRequired = false,
        paid = false,
        outsideWork = false,
        riskLevel = "high",
        organiser = "A prisoner or group of prisoners",
        dpsLocationId = UUID.fromString("4475b5d5-873c-4f88-a5b7-2d20e9224a62"),
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return 4 appointments for a subject access request (Attended, Not attended and Unknown Attendance with one unknown category)`() {
    val response = webTestClient.getSarContent("111222", LocalDate.of(2022, 10, 8), LocalDate.of(2024, 10, 10))

    response.content.appointments containsExactlyInAnyOrder listOf(
      SarAppointment(
        appointmentId = 1,
        prisonCode = PENTONVILLE_PRISON_CODE,
        category = "Education",
        customName = "Education Induction",
        date = LocalDate.of(2022, 10, 12),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(11, 45),
        extraInformation = "Prayer session",
        attended = "Unmarked",
        createdDate = LocalDate.of(2022, 10, 11),
        inCell = true,
        organiser = "Prison staff",
      ),
      SarAppointment(
        appointmentId = 2,
        prisonCode = PENTONVILLE_PRISON_CODE,
        category = "Education",
        customName = "Distance Learning",
        date = LocalDate.of(2022, 10, 13),
        startTime = LocalTime.of(14, 0),
        endTime = LocalTime.of(15, 30),
        extraInformation = null,
        attended = "Yes",
        createdDate = LocalDate.of(2022, 10, 8),
        onWing = true,
        organiser = "An external provider",
      ),
      SarAppointment(
        appointmentId = 3,
        prisonCode = PENTONVILLE_PRISON_CODE,
        category = "Unknown category",
        date = LocalDate.of(2022, 10, 14),
        startTime = LocalTime.of(6, 0),
        endTime = LocalTime.of(8, 30),
        extraInformation = null,
        attended = "No",
        createdDate = LocalDate.of(2022, 10, 9),
        offWing = true,
        cancellationReason = "Created in error",
        cancelledBy = "ABC12D",
      ),
      SarAppointment(
        appointmentId = 4,
        prisonCode = PENTONVILLE_PRISON_CODE,
        category = "Medical - Other",
        customName = "Nurse Clinic",
        date = LocalDate.of(2022, 10, 15),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(12, 0),
        extraInformation = null,
        attended = "No",
        createdDate = LocalDate.of(2022, 10, 13),
        dpsLocationId = UUID.fromString("4475b5d5-873c-4f88-a5b7-2d20e9224a62"),
        cancellationReason = "Cancelled",
        cancelledBy = "XYZ45F",
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
        customName = "Education Induction",
        date = LocalDate.of(2022, 10, 12),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(11, 45),
        extraInformation = "Prayer session",
        attended = "Unmarked",
        createdDate = LocalDate.of(2022, 10, 11),
        inCell = true,
        organiser = "Prison staff",
      ),
    )
  }

  @Sql("classpath:test_data/seed-subject-access-request.sql")
  @Test
  fun `should return one attendance summaries for a subject access request`() {
    val response = webTestClient.getSarContent("A4745DZ", LocalDate.of(2023, 7, 21), LocalDate.of(2023, 7, 22))

    response.content.attendanceSummary containsExactly listOf(
      SarAttendanceSummary(
        attendanceReason = "Attended",
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
        attendanceReason = "Other absence reason not listed",
        count = 1,
      ),
      SarAttendanceSummary(
        attendanceReason = "Prisoner's schedule shows another activity",
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
        attendanceReason = "Other absence reason not listed",
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
        attendanceReason = "Attended",
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
        attendanceReason = "Suspended",
        count = 1,
      ),
      SarAttendanceSummary(
        attendanceReason = "Attended",
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
        allocationId = 4,
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerStatus = "Suspended with pay",
        startDate = LocalDate.of(2022, 10, 10),
        endDate = null,
        activityId = 2,
        activitySummary = "Activity Summary WL",
        payBand = "Pay band 1 (lowest)",
        createdDate = LocalDate.of(2022, 10, 10),
        activityCategoryName = "Industries",
        activityCategoryDescription = "Such as work like recycling, packing or assembly operated by the prison, external firms or charities",
        attendanceRequired = false,
        paid = false,
        outsideWork = false,
        riskLevel = "high",
        organiser = "A prisoner or group of prisoners",
        dpsLocationId = UUID.fromString("4475b5d5-873c-4f88-a5b7-2d20e9224a62"),
      ),
    )

    response.content.waitingListApplications containsExactlyInAnyOrder listOf(
      SarWaitingList(
        waitingListId = 2,
        prisonCode = PENTONVILLE_PRISON_CODE,
        activitySummary = "Activity Summary WL",
        applicationDate = LocalDate.of(2023, 8, 8),
        originator = "Prison staff",
        status = "In progress",
        statusDate = null,
        comments = null,
        createdDate = LocalDate.of(2022, 10, 10),
        activityCategoryName = "Industries",
        activityCategoryDescription = "Such as work like recycling, packing or assembly operated by the prison, external firms or charities",
        attendanceRequired = false,
        paid = false,
        outsideWork = false,
        riskLevel = "high",
        organiser = "A prisoner or group of prisoners",
        dpsLocationId = UUID.fromString("4475b5d5-873c-4f88-a5b7-2d20e9224a62"),
      ),
      SarWaitingList(
        waitingListId = 3,
        prisonCode = PENTONVILLE_PRISON_CODE,
        activitySummary = "Activity Summary WL",
        applicationDate = LocalDate.of(2023, 8, 8),
        originator = "Prison staff",
        status = "Approved",
        statusDate = LocalDate.of(2022, 11, 12),
        comments = "added to the waiting list",
        createdDate = LocalDate.of(2022, 10, 12),
        declinedReason = "Activity ended",
        activityCategoryName = "Industries",
        activityCategoryDescription = "Such as work like recycling, packing or assembly operated by the prison, external firms or charities",
        attendanceRequired = false,
        paid = false,
        outsideWork = false,
        riskLevel = "high",
        organiser = "A prisoner or group of prisoners",
        dpsLocationId = UUID.fromString("4475b5d5-873c-4f88-a5b7-2d20e9224a62"),
      ),
    )

    response.content.appointments containsExactly listOf(
      SarAppointment(
        appointmentId = 1,
        prisonCode = PENTONVILLE_PRISON_CODE,
        category = "Education",
        customName = "Education Induction",
        date = LocalDate.of(2022, 10, 12),
        startTime = LocalTime.of(9, 30),
        endTime = LocalTime.of(11, 45),
        extraInformation = "Prayer session",
        attended = "Unmarked",
        createdDate = LocalDate.of(2022, 10, 11),
        inCell = true,
        organiser = "Prison staff",
      ),
    )
  }

  private fun WebTestClient.getSarContent(prisonerNumber: String, fromDate: LocalDate, toDate: LocalDate) = get()
    .uri("/subject-access-request?prn=$prisonerNumber&fromDate=$fromDate&toDate=$toDate")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisationAsClient(roles = listOf(Role.SUBJECT_ACCESS_REQUEST)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(SubjectAccessRequestContent::class.java)
    .returnResult().responseBody!!
}

data class SubjectAccessRequestContent(val content: SubjectAccessRequestData)
