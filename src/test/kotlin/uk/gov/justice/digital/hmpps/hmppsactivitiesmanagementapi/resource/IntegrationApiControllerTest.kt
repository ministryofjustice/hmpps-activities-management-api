package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toScheduledActivityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityFromDbInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.schedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@WebMvcTest(controllers = [IntegrationApiController::class])
@ContextConfiguration(classes = [IntegrationApiController::class])
class IntegrationApiControllerTest : ControllerTestBase<IntegrationApiController>() {

  @MockitoBean
  private lateinit var attendancesService: AttendancesService

  @MockitoBean
  private lateinit var scheduledInstanceService: ScheduledInstanceService

  @MockitoBean
  private lateinit var activityScheduleService: ActivityScheduleService

  override fun controller() = IntegrationApiController(
    attendancesService,
    scheduledInstanceService,
    activityScheduleService,
  )

  @Nested
  inner class GetPrisonerAttendance {
    val caseNotesApiClient: CaseNotesApiClient = mock()
    val prisonerNumber = "A1234AA"
    val prisonCode = "MDI"
    val attendanceEntity = Attendance(
      scheduledInstance = ScheduledInstance(
        scheduledInstanceId = 1234L,
        activitySchedule = ActivitySchedule(
          activity = Activity(
            activityId = 1234,
            prisonCode = prisonCode,
            activityCategory = ActivityCategory(
              activityCategoryId = 1234L,
              code = "CHAP",
              name = "Chaplaincy",
              description = "Chaplaincy",
            ),
            activityTier = EventTier(
              code = "ABCD",
              description = "Description",
            ),
            attendanceRequired = true,
            inCell = false,
            onWing = true,
            offWing = false,
            pieceWork = false,
            outsideWork = false,
            payPerSession = PayPerSession.H,
            summary = "Summary",
            description = "Description",
            startDate = LocalDate.now(),
            riskLevel = "High",
            createdTime = LocalDateTime.now(),
            createdBy = "Joe Bloggs",
            updatedTime = LocalDateTime.now(),
            updatedBy = "Joe Bloggs",
            isPaid = true,
          ),
          description = "description",
          capacity = 10,
          startDate = LocalDate.now(),
          scheduleWeeks = 1,
        ),
        sessionDate = LocalDate.now(),
        startTime = LocalTime.now(),
        endTime = LocalTime.now().plusHours(1),
        timeSlot = TimeSlot.AM,
      ),
      prisonerNumber = prisonerNumber,
    )

    @Test
    fun `200 response when get attendance for prisoner found`() {
      val history = AttendanceHistory(attendanceHistoryId = 1L, attendance = attendanceEntity, recordedTime = LocalDateTime.now(), recordedBy = "TEST_USER", issuePayment = true, incentiveLevelWarningIssued = false)
      attendanceEntity.addHistory(history)
      val attendance = transform(attendanceEntity, caseNotesApiClient, true)
      whenever(attendancesService.getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1))).thenReturn(listOf(attendance))

      val response = mockMvc.get("/integration-api/attendances/A1234AA?startDate=${LocalDate.now()}&endDate=${LocalDate.now().plusDays(1)}")
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(listOf(attendance)))

      verify(attendancesService).getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1))
    }

    @Test
    fun `200 response when get attendance for prisoner found including prison code`() {
      val history = AttendanceHistory(attendanceHistoryId = 1L, attendance = attendanceEntity, recordedTime = LocalDateTime.now(), recordedBy = "TEST_USER", issuePayment = true, incentiveLevelWarningIssued = false)
      attendanceEntity.addHistory(history)
      val attendance = transform(attendanceEntity, caseNotesApiClient, true)
      whenever(attendancesService.getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1), prisonCode = prisonCode)).thenReturn(listOf(attendance))

      val response = mockMvc.get("/integration-api/attendances/A1234AA?startDate=${LocalDate.now()}&endDate=${LocalDate.now().plusDays(1)}&prisonCode=$prisonCode")
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(listOf(attendance)))

      verify(attendancesService).getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1), prisonCode = prisonCode)
    }

    @Test
    fun `404 response when get attendance for prisoner not found`() {
      whenever(attendancesService.getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1))).thenThrow(EntityNotFoundException("not found"))

      val response = mockMvc.get("/integration-api/attendances/A1234AA?startDate=${LocalDate.now()}&endDate=${LocalDate.now().plusDays(1)}")
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isNotFound() } }
        .andReturn().response

      assertThat(response.contentAsString).contains("not found")

      verify(attendancesService).getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1))
    }
  }

  @Nested
  inner class GetScheduledInstancesForPrisoner {
    @Test
    fun `200 response with scheduled instances`() {
      val results = listOf(activityFromDbInstance()).toScheduledActivityModel()
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      whenever(
        scheduledInstanceService.getActivityScheduleInstancesForPrisonerByDateRange(
          prisonCode = "MDI",
          prisonerNumber = "A1234AA",
          startDate = startDate,
          endDate = endDate,
          slot = TimeSlot.AM,
        ),
      ).thenReturn(results)

      val response = mockMvc.getScheduledInstancesForPrisoner(
        prisonCode = "MDI",
        prisonerNumber = "A1234AA",
        startDate = startDate,
        endDate = endDate,
        slot = TimeSlot.AM,
      )
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(results))

      verify(scheduledInstanceService).getActivityScheduleInstancesForPrisonerByDateRange(
        prisonCode = "MDI",
        prisonerNumber = "A1234AA",
        startDate = startDate,
        endDate = endDate,
        slot = TimeSlot.AM,
      )
    }

    private fun MockMvc.getScheduledInstancesForPrisoner(prisonCode: String, prisonerNumber: String, startDate: LocalDate, endDate: LocalDate, slot: TimeSlot) = get("/integration-api/prisons/$prisonCode/$prisonerNumber/scheduled-instances?startDate=$startDate&endDate=$endDate&slot=$slot")
  }

  @Nested
  inner class GetActivityScheduleSuitabilityCriteria {
    val scheduleId = 1L

    @Test
    fun `200 response with suitability criteria`() {
      val results = schedule(MOORLAND_PRISON_CODE).toModelActivitySuitabilityCriteria()

      whenever(
        activityScheduleService.getSuitabilityCriteria(
          scheduleId = scheduleId
        ),
      ).thenReturn(results)

      val response = mockMvc.getActivityScheduleSuitabilityCriteria(
        scheduleId = scheduleId
      )
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(results))

      verify(activityScheduleService).getSuitabilityCriteria(
        scheduleId = scheduleId
      )
    }

    @Test
    fun `404 response when get activity schedule suitability criteria not found`() {
      whenever(activityScheduleService.getSuitabilityCriteria(scheduleId = scheduleId)).thenThrow(EntityNotFoundException("not found"))

      val response = mockMvc.get("/integration-api/activities/schedule/$scheduleId/suitability-criteria")
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isNotFound() } }
        .andReturn().response

      assertThat(response.contentAsString).contains("not found")

      verify(activityScheduleService).getSuitabilityCriteria(
        scheduleId = scheduleId
      )
    }

    private fun MockMvc.getActivityScheduleSuitabilityCriteria(scheduleId:Long = 1L) = get("/integration-api/activities/schedule/$scheduleId/suitability-criteria")
  }
}
