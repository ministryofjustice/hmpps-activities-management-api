package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateActivitySessionsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateAttendanceRecordsJob

@WebMvcTest(controllers = [JobTriggerController::class])
@ContextConfiguration(classes = [JobTriggerController::class])
class JobTriggerControllerTest : ControllerTestBase<JobTriggerController>() {

  @MockBean
  private lateinit var createActivitySessionsJob: CreateActivitySessionsJob

  @MockBean
  private lateinit var createAttendanceRecordsJob: CreateAttendanceRecordsJob

  override fun controller() = JobTriggerController(createActivitySessionsJob, createAttendanceRecordsJob)

  @Test
  fun `201 response when create activity sessions job triggered`() {
    val response = mockMvc.triggerJob("create-activity-sessions")
      .andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("Activity sessions scheduled")

    verify(createActivitySessionsJob).execute()
  }

  @Test
  fun `201 response when attendance record creation job triggered`() {
    val response = mockMvc.triggerJob("create-attendance-records")
      .andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("Create attendance records triggered")

    verify(createAttendanceRecordsJob).execute()
  }

  private fun MockMvc.triggerJob(jobName: String) = post("/job/$jobName")
}
