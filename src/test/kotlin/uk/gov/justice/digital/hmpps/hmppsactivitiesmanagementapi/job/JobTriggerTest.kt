package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.verify
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ControllerAdvice

@ExtendWith(SpringExtension::class)
@WebMvcTest(controllers = [JobTrigger::class])
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = [JobTrigger::class])
@ActiveProfiles("test")
@WebAppConfiguration
class JobTriggerTest {
  private lateinit var mockMvc: MockMvc

  @MockBean
  private lateinit var createActivitySessionsJob: CreateActivitySessionsJob

  @MockBean
  private lateinit var createAttendanceRecordsJob: CreateAttendanceRecordsJob

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(JobTrigger(createActivitySessionsJob, createAttendanceRecordsJob))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

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
