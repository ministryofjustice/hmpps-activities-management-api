package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.CreateScheduledInstancesJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ManageAllocationsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job.ManageAttendanceRecordsJob
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation

@WebMvcTest(controllers = [JobTriggerController::class])
@ContextConfiguration(classes = [JobTriggerController::class])
class JobTriggerControllerTest : ControllerTestBase<JobTriggerController>() {

  @MockBean
  private lateinit var createScheduledInstancesJob: CreateScheduledInstancesJob

  @MockBean
  private lateinit var manageAttendanceRecordsJob: ManageAttendanceRecordsJob

  @MockBean
  private lateinit var manageAllocationsJob: ManageAllocationsJob

  override fun controller() =
    JobTriggerController(createScheduledInstancesJob, manageAttendanceRecordsJob, manageAllocationsJob)

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
    val response = mockMvc.triggerJob("manage-attendance-records")
      .andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("Manage attendance records triggered")

    verify(manageAttendanceRecordsJob).execute(false)
  }

  @Test
  fun `201 response when attendance job with expiry option is triggered`() {
    val response = mockMvc.triggerJob("manage-attendance-records?withExpiry=true")
      .andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("Manage attendance records triggered")

    verify(manageAttendanceRecordsJob).execute(true)
  }

  @Test
  fun `201 response when manage allocations job triggered for single operation`() {
    val response = mockMvc.triggerJob("manage-allocations", listOf(AllocationOperation.STARTING_TODAY))
      .andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("Manage allocations triggered operations ${setOf(AllocationOperation.STARTING_TODAY)}")

    verify(manageAllocationsJob).execute(listOf(AllocationOperation.STARTING_TODAY))
  }

  @Test
  fun `201 response when manage allocations job triggered for multiple operations`() {
    val response = mockMvc.triggerJob("manage-allocations", listOf(AllocationOperation.STARTING_TODAY, AllocationOperation.DEALLOCATE_ENDING))
      .andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("Manage allocations triggered operations ${setOf(AllocationOperation.STARTING_TODAY, AllocationOperation.DEALLOCATE_ENDING)}")

    verify(manageAllocationsJob).execute(listOf(AllocationOperation.STARTING_TODAY, AllocationOperation.DEALLOCATE_ENDING))
  }

  @Test
  fun `201 response when manage allocations job triggered for duplicate operations`() {
    val response = mockMvc.triggerJob("manage-allocations", listOf(AllocationOperation.DEALLOCATE_EXPIRING, AllocationOperation.DEALLOCATE_EXPIRING))
      .andDo { println() }
      .andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("Manage allocations triggered operations ${setOf(AllocationOperation.DEALLOCATE_EXPIRING)}")

    verify(manageAllocationsJob).execute(listOf(AllocationOperation.DEALLOCATE_EXPIRING))
  }

  private fun MockMvc.triggerJob(jobName: String, mayBeBody: Any? = null) = post("/job/$jobName") {
    contentType = MediaType.APPLICATION_JSON
    content = mayBeBody?.let(mapper::writeValueAsString)
  }
}
