package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.appointment.AppointmentCategoryController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ScheduleReasonEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toAppointmentCategorySummary

@WebMvcTest(controllers = [AppointmentCategoryController::class])
@ContextConfiguration(classes = [AppointmentCategoryController::class])
class AppointmentCategoryControllerTest : ControllerTestBase<AppointmentCategoryController>() {

  @MockBean
  private lateinit var referenceCodeService: ReferenceCodeService

  override fun controller() = AppointmentCategoryController(referenceCodeService)

  @Test
  fun `200 response when get all appointment categories`() {
    val referenceCodes = listOf(appointmentCategoryReferenceCode())

    whenever(referenceCodeService.getScheduleReasons(ScheduleReasonEventType.APPOINTMENT)).thenReturn(referenceCodes)

    val response = mockMvc
      .get("/appointment-categories")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(referenceCodes.toAppointmentCategorySummary()))
  }
}
