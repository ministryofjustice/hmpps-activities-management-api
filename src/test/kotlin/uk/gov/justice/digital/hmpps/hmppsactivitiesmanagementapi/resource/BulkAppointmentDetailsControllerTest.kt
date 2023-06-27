package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.bulkAppointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.BulkAppointmentDetailsService

@WebMvcTest(controllers = [BulkAppointmentDetailsController::class])
@ContextConfiguration(classes = [BulkAppointmentDetailsController::class])
class BulkAppointmentDetailsControllerTest : ControllerTestBase<BulkAppointmentDetailsController>() {
  @MockBean
  private lateinit var bulkAppointmentDetailsService: BulkAppointmentDetailsService

  override fun controller() = BulkAppointmentDetailsController(bulkAppointmentDetailsService)

  @Test
  fun `200 response when get bulk appointment details by valid id`() {
    val details = bulkAppointmentDetails()

    whenever(bulkAppointmentDetailsService.getBulkAppointmentDetailsById(1)).thenReturn(details)

    val response = mockMvc.getBulkAppointmentDetailsById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(details))

    verify(bulkAppointmentDetailsService).getBulkAppointmentDetailsById(1)
  }

  @Test
  fun `404 response when get bulk appointment details by invalid id`() {
    whenever(bulkAppointmentDetailsService.getBulkAppointmentDetailsById(-1)).thenThrow(EntityNotFoundException("Bulk appointment -1 not found"))

    val response = mockMvc.getBulkAppointmentDetailsById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Bulk appointment -1 not found")

    verify(bulkAppointmentDetailsService).getBulkAppointmentDetailsById(-1)
  }

  private fun MockMvc.getBulkAppointmentDetailsById(id: Long) = get("/bulk-appointment-details/{bulkAppointmentId}", id)
}
