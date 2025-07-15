package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.StringStartsWith
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.weeksAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toScheduledActivityModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityFromDbInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchResultModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.earliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBandsLowMediumHigh
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.schedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivitySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityScheduleService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AttendancesService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerScheduledEventsFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledEventService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ScheduledInstanceService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentSearchService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.AttendanceReasonService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelPrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModelSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.security.Principal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

@WebMvcTest(controllers = [IntegrationApiController::class])
@ContextConfiguration(classes = [IntegrationApiController::class])
class IntegrationApiControllerTest : ControllerTestBase<IntegrationApiController>() {

  @MockitoBean
  private lateinit var attendancesService: AttendancesService

  @MockitoBean
  private lateinit var scheduledInstanceService: ScheduledInstanceService

  @MockitoBean
  private lateinit var attendanceReasonService: AttendanceReasonService

  @MockitoBean
  private lateinit var activityService: ActivityService

  @MockitoBean
  private lateinit var activityScheduleService: ActivityScheduleService

  @MockitoBean
  private lateinit var waitingListService: WaitingListService

  @MockitoBean
  private lateinit var prisonRegimeService: PrisonRegimeService

  @MockitoBean
  private lateinit var scheduledEventService: ScheduledEventService

  @MockitoBean
  private lateinit var referenceCodeService: ReferenceCodeService

  @MockitoBean
  private lateinit var appointmentSearchService: AppointmentSearchService

  override fun controller() = IntegrationApiController(
    attendancesService,
    scheduledInstanceService,
    scheduledEventService,
    referenceCodeService,
    attendanceReasonService,
    activityService,
    activityScheduleService,
    waitingListService,
    prisonRegimeService,
    appointmentSearchService,
  )

  @Nested
  inner class GetDeallocationReasons {
    @Test
    fun `200 response when get deallocation reasons`() {
      val response = mockMvc.get("/integration-api/allocations/deallocation-reasons")
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).contains(
        DeallocationReason.COMPLETED.name,
        DeallocationReason.HEALTH.name,
        DeallocationReason.OTHER.name,
        DeallocationReason.SECURITY.name,
        DeallocationReason.TRANSFERRED.name,
        DeallocationReason.WITHDRAWN_OWN.name,
        DeallocationReason.WITHDRAWN_STAFF.name,
      )

      assertThat(response.contentAsString).doesNotContain(
        DeallocationReason.DIED.name,
        DeallocationReason.ENDED.name,
        DeallocationReason.EXPIRED.name,
        DeallocationReason.RELEASED.name,
        DeallocationReason.TEMPORARILY_RELEASED.name,
      )
    }
  }

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

      val response = mockMvc.get("/integration-api/attendances/prisoner/A1234AA?startDate=${LocalDate.now()}&endDate=${LocalDate.now().plusDays(1)}")
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

      val response = mockMvc.get("/integration-api/attendances/prisoner/A1234AA?startDate=${LocalDate.now()}&endDate=${LocalDate.now().plusDays(1)}&prisonCode=$prisonCode")
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(listOf(attendance)))

      verify(attendancesService).getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1), prisonCode = prisonCode)
    }

    @Test
    fun `404 response when get attendance for prisoner not found`() {
      whenever(attendancesService.getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1))).thenThrow(EntityNotFoundException("not found"))

      val response = mockMvc.get("/integration-api/attendances/prisoner/A1234AA?startDate=${LocalDate.now()}&endDate=${LocalDate.now().plusDays(1)}")
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isNotFound() } }
        .andReturn().response

      assertThat(response.contentAsString).contains("not found")

      verify(attendancesService).getPrisonerAttendance(prisonerNumber = prisonerNumber, startDate = LocalDate.now(), endDate = LocalDate.now().plusDays(1))
    }
  }

  @Nested
  inner class GetAttendanceById {
    @Test
    fun `200 response when get attendance by ID found`() {
      val caseNotesApiClient: CaseNotesApiClient = mock()
      val attendanceEntity = attendance()
      val history = AttendanceHistory(attendanceHistoryId = 1L, attendance = attendanceEntity, recordedTime = LocalDateTime.now(), recordedBy = "TEST_USER", issuePayment = true, incentiveLevelWarningIssued = false)
      attendanceEntity.addHistory(history)
      val attendance = transform(attendanceEntity, caseNotesApiClient, true)

      whenever(attendancesService.getAttendanceById(1)).thenReturn(attendance)

      val response = mockMvc.getAttendanceById("1")
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(attendance))

      verify(attendancesService).getAttendanceById(1)
    }

    @Test
    fun `404 response when get attendance by ID not found`() {
      whenever(attendancesService.getAttendanceById(2)).thenThrow(EntityNotFoundException("not found"))

      val response = mockMvc.getAttendanceById("2")
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isNotFound() } }
        .andReturn().response

      assertThat(response.contentAsString).contains("Not found")

      verify(attendancesService).getAttendanceById(2)
    }

    private fun MockMvc.getAttendanceById(attendanceId: String) = get("/integration-api/attendances/$attendanceId")
  }

  @Nested
  inner class GetAttendanceReasons {
    @Test
    fun `200 response when get attendance reasons`() {
      val expectedModel = listOf(attendanceReason().toModel())

      whenever(attendanceReasonService.getAll()).thenReturn(listOf(attendanceReason().toModel()))

      val response = mockMvc
        .get("/integration-api/attendance-reasons")
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

      verify(attendanceReasonService).getAll()
    }
  }

  @Nested
  inner class GetActivitySchedules {
    @Test
    fun `200 response when get activity schedules`() {
      val expectedModel = listOf(
        ActivityScheduleLite(
          id = 1,
          description = "schedule description",
          internalLocation = InternalLocation(1, "EDU-ROOM-1", "Education - R1"),
          capacity = 20,
          activity = ActivityLite(
            id = 12L,
            prisonCode = "MDI",
            attendanceRequired = true,
            inCell = false,
            onWing = false,
            offWing = false,
            pieceWork = false,
            outsideWork = false,
            payPerSession = uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession.H,
            summary = "Maths",
            description = "Beginner maths",
            riskLevel = "High",
            category = ModelActivityCategory(
              id = 1L,
              code = "EDUCATION",
              name = "Education",
              description = "Such as classes in English, maths, construction and computer skills",
            ),
            capacity = 20,
            allocated = 10,
            createdTime = LocalDateTime.now(),
            activityState = ActivityState.LIVE,
            paid = true,
          ),
          slots = listOf(
            ActivityScheduleSlot(
              id = 1L,
              timeSlot = TimeSlot.AM,
              weekNumber = 1,
              startTime = LocalTime.of(10, 20),
              endTime = LocalTime.of(10, 20),
              daysOfWeek = listOf("Mon"),
              mondayFlag = true,
              tuesdayFlag = false,
              wednesdayFlag = false,
              thursdayFlag = false,
              fridayFlag = false,
              saturdayFlag = false,
              sundayFlag = false,
            ),
          ),
          startDate = LocalDate.now(),
          scheduleWeeks = 1,
          usePrisonRegimeTime = true,
        ),
      )

      whenever(activityService.getSchedulesForActivity(1)).thenReturn(expectedModel)

      val response = mockMvc.getActivitySchedules(1)
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

      verify(activityService).getSchedulesForActivity(1)
    }

    @Test
    fun `404 response when get activity schedules and activity id not found`() {
      whenever(activityService.getSchedulesForActivity(2)).thenThrow(EntityNotFoundException("not found"))

      val response = mockMvc.getActivitySchedules(2)
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isNotFound() } }
        .andReturn().response

      assertThat(response.contentAsString).contains("Not found")

      verify(activityService).getSchedulesForActivity(2)
    }
    private fun MockMvc.getActivitySchedules(id: Long) = get("/integration-api/activities/{activityId}/schedules", id)
  }

  @Nested
  inner class GetScheduleById {
    @Test
    fun `200 response when get schedule by schedule identifier with earliest session date default`() {
      val expected = activityEntity().schedules().first().copy(1).toModelSchedule()

      whenever(activityScheduleService.getScheduleById(1, 4.weeksAgo())) doReturn expected

      val response = mockMvc.getScheduleById(1)
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      response.contentAsString isEqualTo mapper.writeValueAsString(expected)
    }

    @Test
    fun `200 response when get schedule by schedule identifier with earliest session date specified`() {
      val expected = activityEntity().schedules().first().copy(1).toModelSchedule()

      whenever(activityScheduleService.getScheduleById(1, 4.weeksAgo())) doReturn expected

      val response = mockMvc.getScheduleById(1, 4.weeksAgo())
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      response.contentAsString isEqualTo mapper.writeValueAsString(expected)
    }

    @Test
    fun `404 response when get schedule by id not found`() {
      whenever(activityScheduleService.getScheduleById(eq(-99), any(), eq(false))).thenThrow(EntityNotFoundException("not found"))

      val response = mockMvc.getScheduleById(-99)
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isNotFound() } }
        .andReturn().response

      assertThat(response.contentAsString).contains("not found")
    }

    private fun MockMvc.getScheduleById(scheduleId: Long, earliestSessionDate: LocalDate? = null) = get("/integration-api/schedules/$scheduleId${earliestSessionDate?.let { "?earliestSessionDate=$it" } ?: ""}")
  }

  @Nested
  inner class SearchWaitingLists {
    @Test
    fun `200 response when searching waiting list application`() {
      val request = WaitingListSearchRequest()
      val waitingListApplication = waitingList().toModel(earliestReleaseDate())
      val pagedResult = PageImpl(listOf(waitingListApplication), Pageable.ofSize(1), 1)

      whenever(
        waitingListService.searchWaitingLists(
          "MDI",
          request,
          0,
          50,
        ),
      ).thenReturn(pagedResult)

      val response = mockMvc.post("/integration-api/waiting-list-applications/MDI/search") {
        contentType = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(request)
      }
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      JSONAssert.assertEquals((response.contentAsString), mapper.writeValueAsString(pagedResult), false)
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
          scheduleId = scheduleId,
        ),
      ).thenReturn(results)

      val response = mockMvc.getActivityScheduleSuitabilityCriteria(
        scheduleId = scheduleId,
      )
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(results))

      verify(activityScheduleService).getSuitabilityCriteria(
        scheduleId = scheduleId,
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
        scheduleId = scheduleId,
      )
    }

    private fun MockMvc.getActivityScheduleSuitabilityCriteria(scheduleId: Long = 1L) = get("/integration-api/activities/schedule/$scheduleId/suitability-criteria")
  }

  @Nested
  inner class GetActivities {
    @Test
    fun `200 response when get activities`() {
      val expectedModel = listOf(
        ActivitySummary(
          id = 1,
          activityName = "Book club",
          category = ModelActivityCategory(
            id = 1L,
            code = "LEISURE_SOCIAL",
            name = "Leisure and social",
            description = "Such as association, library time and social clubs, like music or art",
          ),
          capacity = 20,
          allocated = 10,
          waitlisted = 3,
          createdTime = LocalDateTime.now(),
          activityState = ActivityState.LIVE,
        ),
      )

      whenever(activityService.getActivitiesInPrison(MOORLAND_PRISON_CODE, true)).thenReturn(
        expectedModel,
      )

      val response = mockMvc.getActivities(MOORLAND_PRISON_CODE)
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }.andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))

      verify(activityService, times(1)).getActivitiesInPrison(MOORLAND_PRISON_CODE, true)
    }

    private fun MockMvc.getActivities(prisonCode: String) = get("/integration-api/prison/{prisonCode}/activities", prisonCode)
  }

  @Nested
  inner class GetPrisonPayBands {
    @Test
    fun `200 response when get pay bands by Moorland prison code`() {
      val prisonPayBands = prisonPayBandsLowMediumHigh().map { it.toModelPrisonPayBand() }

      whenever(prisonRegimeService.getPayBandsForPrison(MOORLAND_PRISON_CODE)).thenReturn(prisonPayBands)

      val response = mockMvc.getPrisonPayBandsBy(MOORLAND_PRISON_CODE)
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(prisonPayBands))

      verify(prisonRegimeService).getPayBandsForPrison(MOORLAND_PRISON_CODE)
    }

    private fun MockMvc.getPrisonPayBandsBy(prisonCode: String) = get("/integration-api/prison/$prisonCode/prison-pay-bands")
  }

  @Nested
  inner class GetPrisonRegimeByPrisonCode {
    @Test
    fun `200 response when get prison by code found`() {
      val prisonRegime = transform(prisonRegime(), DayOfWeek.MONDAY)

      whenever(prisonRegimeService.getPrisonRegimeByPrisonCode(PENTONVILLE_PRISON_CODE)).thenReturn(listOf(prisonRegime))

      val response = mockMvc.getPrisonRegimeByPrisonCode(PENTONVILLE_PRISON_CODE)
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(listOf(prisonRegime)))

      verify(prisonRegimeService).getPrisonRegimeByPrisonCode(PENTONVILLE_PRISON_CODE)
    }
    private fun MockMvc.getPrisonRegimeByPrisonCode(prisonCode: String) = get("/integration-api/prison/prison-regime/$prisonCode")
  }

  @Nested
  inner class GetScheduledEventsForSinglePrisoner {
    @Test
    fun `getScheduledEventsForSinglePrisoner - 200 response with events`() {
      val result = PrisonerScheduledEventsFixture.instance()
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      whenever(
        scheduledEventService.getScheduledEventsForSinglePrisoner(
          "MDI",
          "G1234GG",
          LocalDateRange(startDate, endDate),
          null,
          referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
        ),
      ).thenReturn(result)

      val response = mockMvc.getScheduledEventsForSinglePrisoner(
        "MDI",
        "G1234GG",
        startDate,
        endDate,
      )
        .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
        .andExpect { status { isOk() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(result))

      verify(scheduledEventService).getScheduledEventsForSinglePrisoner(
        "MDI",
        "G1234GG",
        LocalDateRange(startDate, endDate),
        null,
        referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
      )
    }

    @Test
    fun `getScheduledEventsForSinglePrisoner - Error response when service throws exception`() {
      val result = this::class.java.getResource("/__files/error-500.json")?.readText()
      val startDate = LocalDate.of(2022, 10, 1)
      val endDate = LocalDate.of(2022, 11, 5)

      whenever(
        scheduledEventService.getScheduledEventsForSinglePrisoner(
          "MDI",
          "G1234GG",
          LocalDateRange(startDate, endDate),
          null,
          referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
        ),
      ).thenThrow(RuntimeException("Error"))

      val response = mockMvc.getScheduledEventsForSinglePrisoner(
        "MDI",
        "G1234GG",
        startDate,
        endDate,
      )
        .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
        .andExpect { status { is5xxServerError() } }
        .andReturn().response

      assertThat(response.contentAsString + "\n").isEqualTo(result)

      verify(scheduledEventService).getScheduledEventsForSinglePrisoner(
        "MDI",
        "G1234GG",
        LocalDateRange(startDate, endDate),
        null,
        referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
      )
    }

    @Test
    fun `getScheduledEventsForSinglePrisoner - 200 response when date range equals 3 months`() {
      val result = PrisonerScheduledEventsFixture.instance()
      val startDate = LocalDate.of(2022, 11, 1)
      val endDate = LocalDate.of(2023, 2, 1)

      whenever(
        scheduledEventService.getScheduledEventsForSinglePrisoner(
          "MDI",
          "G1234GG",
          LocalDateRange(startDate, endDate),
          null,
          referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
        ),
      ).thenReturn(result)

      mockMvc.get("/integration-api/scheduled-events/prison/MDI") {
        param("prisonerNumber", "G1234GG")
        param("startDate", "2022-11-01")
        param("endDate", "2023-02-01")
      }
        .andDo { print() }
        .andExpect {
          status {
            isOk()
          }
          content {
            contentType(MediaType.APPLICATION_JSON)
          }
        }
    }

    @Test
    fun `getScheduledEventsForSinglePrisoner - 400 response when end date missing`() {
      mockMvc.get("/integration-api/scheduled-events/prison/MDI") {
        param("prisonerNumber", "G1234GG")
        param("startDate", "2022-10-01")
      }
        .andDo { print() }
        .andExpect {
          status {
            is4xxClientError()
          }
          content {
            contentType(MediaType.APPLICATION_JSON)
            jsonPath("$.userMessage") {
              value("Required request parameter 'endDate' for method parameter type LocalDate is not present")
            }
          }
        }
    }

    @Test
    fun `getScheduledEventsForSinglePrisoner - 400 response when start date missing`() {
      mockMvc.get("/integration-api/scheduled-events/prison/MDI") {
        param("prisonerNumber", "G1234GG")
        param("endDate", "2022-10-01")
      }
        .andDo { print() }
        .andExpect {
          status {
            is4xxClientError()
          }
          content {
            contentType(MediaType.APPLICATION_JSON)
            jsonPath("$.userMessage") {
              value("Required request parameter 'startDate' for method parameter type LocalDate is not present")
            }
          }
        }
    }

    @Test
    fun `getScheduledEventsForSinglePrisoner - 400 response when start date incorrect format`() {
      mockMvc.get("/integration-api/scheduled-events/prison/MDI") {
        param("prisonerNumber", "G1234GG")
        param("startDate", "01/10/2022")
      }
        .andDo { print() }
        .andExpect {
          status {
            is4xxClientError()
          }
          content {
            contentType(MediaType.APPLICATION_JSON)
            jsonPath("$.userMessage") {
              value("Error converting 'startDate' (01/10/2022): Method parameter 'startDate': Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
            }
          }
        }
    }

    @Test
    fun `getScheduledEventsForSinglePrisoner - 400 response when end date incorrect format`() {
      mockMvc.get("/integration-api/scheduled-events/prison/MDI") {
        param("prisonerNumber", "G1234GG")
        param("startDate", "2022-10-01")
        param("endDate", "01/10/2022")
      }
        .andDo { print() }
        .andExpect {
          status {
            is4xxClientError()
          }
          content {
            contentType(MediaType.APPLICATION_JSON)
            jsonPath("$.userMessage") {
              value("Error converting 'endDate' (01/10/2022): Method parameter 'endDate': Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
            }
          }
        }
    }

    @Test
    fun `getScheduledEventsForSinglePrisoner - 400 response when date range exceeds 3 months`() {
      mockMvc.get("/integration-api/scheduled-events/prison/MDI") {
        param("prisonerNumber", "G1234GG")
        param("startDate", "2022-11-01")
        param("endDate", "2023-02-02")
      }
        .andDo { print() }
        .andExpect {
          status {
            is4xxClientError()
          }
          content {
            contentType(MediaType.APPLICATION_JSON)
            jsonPath("$.userMessage") {
              value("Validation failure: Date range cannot exceed 3 months")
            }
          }
        }
    }
    private fun MockMvc.getScheduledEventsForSinglePrisoner(
      prisonCode: String,
      prisonerNumber: String,
      startDate: LocalDate,
      endDate: LocalDate,
    ) = get("/integration-api/scheduled-events/prison/$prisonCode?prisonerNumber=$prisonerNumber&startDate=$startDate&endDate=$endDate")
  }

  @Nested
  inner class GetScheduledEventsForMultiplePrisoners {
    @Test
    fun `getScheduledEventsForMultiplePrisoners - 200 response with events`() {
      val prisonerNumbers = setOf("G1234GG")
      val result = PrisonerScheduledEventsFixture.instance()

      whenever(
        scheduledEventService.getScheduledEventsForMultiplePrisoners(
          "MDI",
          prisonerNumbers,
          LocalDate.of(2022, 10, 1),
          TimeSlot.AM,
          referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
        ),
      ).thenReturn(result)

      val response =
        mockMvc.getScheduledEventsForMultiplePrisoners(
          "MDI",
          prisonerNumbers,
          LocalDate.of(2022, 10, 1),
          TimeSlot.AM.name,
        )
          .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
          .andExpect { status { isOk() } }
          .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(result))

      verify(scheduledEventService).getScheduledEventsForMultiplePrisoners(
        "MDI",
        prisonerNumbers,
        LocalDate.of(2022, 10, 1),
        TimeSlot.AM,
        referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
      )
    }

    @Test
    fun `getScheduledEventsForMultiplePrisoners - Error response when service throws exception`() {
      val prisonerNumbers = setOf("G1234GG")
      val result = this::class.java.getResource("/__files/error-500.json")?.readText()

      whenever(
        scheduledEventService.getScheduledEventsForMultiplePrisoners(
          "MDI",
          prisonerNumbers,
          LocalDate.of(2022, 10, 1),
          TimeSlot.AM,
          referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
        ),
      ).thenThrow(RuntimeException("Error"))

      val response = mockMvc.getScheduledEventsForMultiplePrisoners(
        "MDI",
        prisonerNumbers,
        LocalDate.of(2022, 10, 1),
        TimeSlot.AM.name,
      )
        .andExpect { content { contentType(MediaType.APPLICATION_JSON) } }
        .andExpect { status { is5xxServerError() } }
        .andReturn().response

      assertThat(response.contentAsString + "\n").isEqualTo(result)

      verify(scheduledEventService).getScheduledEventsForMultiplePrisoners(
        "MDI",
        prisonerNumbers,
        LocalDate.of(2022, 10, 1),
        TimeSlot.AM,
        referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY),
      )
    }

    @Test
    fun `getScheduledEventsForMultiplePrisoners - 400 response when no date provided`() {
      val prisonerNumbers = setOf("G1234GG")

      mockMvc.post("/integration-api/scheduled-events/prison/MDI") {
        accept = MediaType.APPLICATION_JSON
        contentType = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(
          prisonerNumbers,
        )
      }
        .andDo { print() }
        .andExpect {
          status {
            isBadRequest()
          }
          content {
            contentType(MediaType.APPLICATION_JSON)
            jsonPath("$.userMessage") {
              value("Required request parameter 'date' for method parameter type LocalDate is not present")
            }
          }
        }
    }

    @Test
    fun `getScheduledEventsForMultiplePrisoners - 400 response when no prisoner numbers are provided`() {
      mockMvc.post("/integration-api/scheduled-events/prison/MDI?date=2022-12-14") {
        accept = MediaType.APPLICATION_JSON
        contentType = MediaType.APPLICATION_JSON
      }
        .andDo { print() }
        .andExpect {
          status {
            is4xxClientError()
          }
          content {
            contentType(MediaType.APPLICATION_JSON)
            jsonPath("$.userMessage") {
              value(StringStartsWith("Required request body is missing:"))
            }
          }
        }
    }

    @Test
    fun `getScheduledEventsForMultiplePrisoners - 400 response when date incorrect format`() {
      val prisonerNumbers = setOf("G1234GG")
      mockMvc.post("/integration-api/scheduled-events/prison/MDI?date=20/12/2022") {
        accept = MediaType.APPLICATION_JSON
        contentType = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(
          prisonerNumbers,
        )
      }
        .andDo { print() }
        .andExpect {
          status {
            is4xxClientError()
          }
          content {
            contentType(MediaType.APPLICATION_JSON)
            jsonPath("$.userMessage") {
              value("Error converting 'date' (20/12/2022): Method parameter 'date': Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
            }
          }
        }
    }

    @Test
    fun `getScheduledEventsForMultiplePrisoners - 400 response when time slot is an incorrect format`() {
      val prisonerNumbers = setOf("G1234GG")
      mockMvc.post("/integration-api/scheduled-events/prison/MDI?date=2022-12-01&timeSlot=AF") {
        accept = MediaType.APPLICATION_JSON
        contentType = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(
          prisonerNumbers,
        )
      }
        .andDo { print() }
        .andExpect {
          status {
            is4xxClientError()
          }
          content {
            contentType(MediaType.APPLICATION_JSON)
            jsonPath("$.userMessage") {
              value("Error converting 'timeSlot' (AF): Method parameter 'timeSlot': Failed to convert value of type 'java.lang.String' to required type 'uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot'")
            }
          }
        }
    }

    private fun MockMvc.getScheduledEventsForMultiplePrisoners(
      prisonCode: String,
      prisonerNumbers: Set<String>,
      date: LocalDate,
      timeSlot: String,
    ) = post("/integration-api/scheduled-events/prison/$prisonCode?date=$date&timeSlot=$timeSlot") {
      accept = MediaType.APPLICATION_JSON
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        prisonerNumbers,
      )
    }
  }

  @Nested
  inner class SearchAppointments {
    @Test
    fun `202 accepted response when search appointments with valid json`() {
      val request = AppointmentSearchRequest(startDate = LocalDate.now())
      val expectedResponse = listOf(appointmentSearchResultModel())

      val mockPrincipal: Principal = mock()

      whenever(appointmentSearchService.searchAppointments("TPR", request, mockPrincipal)).thenReturn(expectedResponse)

      val response = mockMvc.searchAppointments("TPR", request, mockPrincipal)
        .andDo { print() }
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isAccepted() } }
        .andReturn().response

      assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))
    }

    private fun MockMvc.searchAppointments(
      prisonCode: String,
      request: AppointmentSearchRequest,
      principal: Principal,
    ) = post("/integration-api/appointments/{prisonCode}/search", prisonCode) {
      this.principal = principal
      contentType = MediaType.APPLICATION_JSON
      content = mapper.writeValueAsBytes(
        request,
      )
    }
  }
}
