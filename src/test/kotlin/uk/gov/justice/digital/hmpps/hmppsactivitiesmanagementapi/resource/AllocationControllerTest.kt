package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationsService

@WebMvcTest(controllers = [AllocationController::class])
@ContextConfiguration(classes = [AllocationController::class])
class AllocationControllerTest : ControllerTestBase<AllocationController>() {

  @MockBean
  private lateinit var allocationsService: AllocationsService

  override fun controller() = AllocationController(allocationsService)

  @Test
  fun `200 response when get allocation by ID found`() {
    val allocation = allocation().toModel()

    whenever(allocationsService.getAllocationById(1)).thenReturn(allocation)

    val response = mockMvc.getAllocationById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(allocation))

    verify(allocationsService).getAllocationById(1)
  }

  @Test
  fun `404 response when get allocation by ID found`() {
    whenever(allocationsService.getAllocationById(any())).thenThrow(EntityNotFoundException("not found"))

    mockMvc.getAllocationById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
  }

  @Test
  fun `200 response when get deallocation reasons`() {
    val response = mockMvc.get("/allocations/deallocation-reasons")
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).contains(DeallocationReason.OTHER.name)
    assertThat(response.contentAsString).contains(DeallocationReason.PERSONAL.name)
    assertThat(response.contentAsString).contains(DeallocationReason.PROBLEM.name)
    assertThat(response.contentAsString).contains(DeallocationReason.REMOVED.name)
    assertThat(response.contentAsString).contains(DeallocationReason.SECURITY.name)
    assertThat(response.contentAsString).contains(DeallocationReason.UNACCEPTABLE_ATTENDANCE.name)
    assertThat(response.contentAsString).contains(DeallocationReason.UNACCEPTABLE_BEHAVIOUR.name)
    assertThat(response.contentAsString).contains(DeallocationReason.WITHDRAWN.name)

    assertThat(response.contentAsString).doesNotContain(DeallocationReason.DIED.name)
    assertThat(response.contentAsString).doesNotContain(DeallocationReason.ENDED.name)
    assertThat(response.contentAsString).doesNotContain(DeallocationReason.EXPIRED.name)
    assertThat(response.contentAsString).doesNotContain(DeallocationReason.RELEASED.name)
    assertThat(response.contentAsString).doesNotContain(DeallocationReason.TEMPORARY_ABSENCE.name)
  }

  private fun MockMvc.getAllocationById(id: Long) = get("/allocations/id/{allocationId}", id)
}
