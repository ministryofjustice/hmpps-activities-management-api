package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ActivityMetricsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.AppointmentMetricsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateScheduledInstancesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.FixZeroPayJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ManageAllocationsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ManageAttendanceRecordsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.PurposefulActivityReportsJob
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@WebMvcTest(controllers = [JobTriggerController::class])
@ContextConfiguration(classes = [JobTriggerController::class])
class JobTriggerControllerTest : ControllerTestBase<JobTriggerController>() {

  @MockitoBean
  private lateinit var createScheduledInstancesJob: CreateScheduledInstancesJob

  @MockitoBean
  private lateinit var manageAttendanceRecordsJob: ManageAttendanceRecordsJob

  @MockitoBean
  private lateinit var manageAllocationsJob: ManageAllocationsJob

  @MockitoBean
  private lateinit var activityMetricsJob: ActivityMetricsJob

  @MockitoBean
  private lateinit var appointmentsMetricsJob: AppointmentMetricsJob

  private lateinit var purposefulActivityReportsJob: PurposefulActivityReportsJob

  @MockitoBean
  private lateinit var fixZeroPayJob: FixZeroPayJob

  @MockitoBean
  private lateinit var clock: Clock

  @BeforeEach
  fun init() {
    whenever(clock.instant()).thenReturn(LocalDateTime.now().toInstant(ZoneOffset.UTC))
    whenever(clock.zone).thenReturn(ZoneId.of("UTC"))
  }

  override fun controller() =
    JobTriggerController(createScheduledInstancesJob, manageAttendanceRecordsJob, manageAllocationsJob, activityMetricsJob, appointmentsMetricsJob, fixZeroPayJob, clock, purposefulActivityReportsJob)

  @Test
  fun `201 response when create activity sessions job triggered`() {
    val response =
      mockMvc.triggerJob("create-scheduled-instances").andExpect { status { isCreated() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo("Create scheduled instances triggered")

    verify(createScheduledInstancesJob).execute()
  }

  @Test
  fun `201 response when attendance record creation job triggered`() {
    val response =
      mockMvc.triggerJob("manage-attendance-records").andExpect { status { isCreated() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo("Manage attendance records triggered")

    verify(manageAttendanceRecordsJob).execute(date = LocalDate.now(), mayBePrisonCode = null, withExpiry = false)
  }

  @Test
  fun `201 response when attendance record creation job with date option is triggered`() {
    val response =
      mockMvc.triggerJob("manage-attendance-records?date=2023-11-16").andExpect { status { isCreated() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo("Manage attendance records triggered")

    verify(manageAttendanceRecordsJob).execute(date = LocalDate.of(2023, 11, 16), mayBePrisonCode = null, withExpiry = false)
  }

  @Test
  fun `201 response when attendance record creation job with prison code option is triggered`() {
    val response =
      mockMvc.triggerJob("manage-attendance-records?prisonCode=$PENTONVILLE_PRISON_CODE").andExpect { status { isCreated() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo("Manage attendance records triggered")

    verify(manageAttendanceRecordsJob).execute(date = LocalDate.now(), mayBePrisonCode = PENTONVILLE_PRISON_CODE, withExpiry = false)
  }

  @Test
  fun `201 response when attendance job with expiry option is triggered`() {
    val response = mockMvc.triggerJob("manage-attendance-records?withExpiry=true").andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("Manage attendance records triggered")

    verify(manageAttendanceRecordsJob).execute(date = LocalDate.now(), withExpiry = true)
  }

  @ParameterizedTest(name = " where withActivate = {0}, withDeallocateEnding={1}, withDeallocateExpiring={2}")
  @MethodSource("allocationArgs")
  fun `201 response when manage allocations job triggered`(withActivate: Boolean, withDeallocateEnding: Boolean, withDeallocateExpiring: Boolean) {
    val response = mockMvc.triggerJob(jobName = "manage-allocations?withActivate=$withActivate&withDeallocateEnding=$withDeallocateEnding&withDeallocateExpiring=$withDeallocateExpiring")
      .andExpect { status { isCreated() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo("Manage allocations triggered operations")

    verify(manageAllocationsJob).execute(withActivate = withActivate, withDeallocateEnding = withDeallocateEnding, withDeallocateExpiring = withDeallocateExpiring)
  }

  companion object {
    @JvmStatic
    fun allocationArgs(): List<Arguments> = listOf(
      Arguments.of(true, false, false),
      Arguments.of(false, true, false),
      Arguments.of(false, false, true),
      Arguments.of(true, true, false),
      Arguments.of(true, false, true),
      Arguments.of(false, true, true),
      Arguments.of(true, true, true),
    )
  }

  @Test
  fun `201 response when activities metrics job triggered`() {
    val response = mockMvc.triggerJob(jobName = "activities-metrics")
      .andExpect { status { isCreated() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo("Activity metrics job triggered")

    verify(activityMetricsJob).execute()
  }

  @Test
  fun `202 response when appointments metrics job triggered`() {
    val response = mockMvc.triggerJob(jobName = "appointments-metrics")
      .andExpect { status { isAccepted() } }.andReturn().response

    assertThat(response.contentAsString).isEqualTo("Appointments metrics job triggered")

    verify(appointmentsMetricsJob).execute()
  }

  private fun MockMvc.triggerJob(jobName: String) =
    post("/job/$jobName") {
      contentType = MediaType.APPLICATION_JSON
    }
}
