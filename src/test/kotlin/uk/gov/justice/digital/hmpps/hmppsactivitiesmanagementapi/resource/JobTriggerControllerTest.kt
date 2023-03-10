package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateAttendanceRecordsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateScheduledInstancesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.OffenderDeallocationJob

@WebMvcTest(controllers = [JobTriggerController::class])
@ContextConfiguration(classes = [JobTriggerController::class])
class JobTriggerControllerTest : ControllerTestBase<JobTriggerController>() {

  @MockBean
  private lateinit var createScheduledInstancesJob: CreateScheduledInstancesJob

  @MockBean
  private lateinit var createAttendanceRecordsJob: CreateAttendanceRecordsJob

  @MockBean
  private lateinit var offenderDeallocationJob: OffenderDeallocationJob

  override fun controller() = JobTriggerController(createScheduledInstancesJob, createAttendanceRecordsJob, offenderDeallocationJob)

  @Test
  fun `201 response when create activity sessions job triggered`() {
    val response = mockMvc.triggerJob("create-scheduled-instances")
      .andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("Create scheduled instances triggered")

    verify(createScheduledInstancesJob).execute()
  }

  @Test
  fun `201 response when attendance record creation job triggered`() {
    val response = mockMvc.triggerJob("create-attendance-records")
      .andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("Create attendance records triggered")

    verify(createAttendanceRecordsJob).execute()
  }

  @Test
  fun `201 response when deallocate offenders job triggered`() {
    val response = mockMvc.triggerJob("deallocate-offenders")
      .andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("Deallocate offenders when end dates are reached triggered")

    verify(offenderDeallocationJob).execute()
  }

  private fun MockMvc.triggerJob(jobName: String) = post("/job/$jobName")
}
