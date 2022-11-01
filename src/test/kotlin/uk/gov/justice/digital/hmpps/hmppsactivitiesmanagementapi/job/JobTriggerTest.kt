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

  @BeforeEach
  fun before() {
    mockMvc = MockMvcBuilders
      .standaloneSetup(JobTrigger(createActivitySessionsJob))
      .setControllerAdvice(ControllerAdvice())
      .build()
  }

  @Test
  fun `201 response when get activity by ID found`() {
    val response = mockMvc.triggerCreateActivitySessionsJob()
      .andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("Activity sessions scheduled")

    verify(createActivitySessionsJob).execute()
  }

  private fun MockMvc.triggerCreateActivitySessionsJob() = post("/job/create-activity-sessions")
}
