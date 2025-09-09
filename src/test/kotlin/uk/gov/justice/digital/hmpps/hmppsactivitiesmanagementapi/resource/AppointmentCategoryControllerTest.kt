package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentParentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.CategoryStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCategoryRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AppointmentCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.appointment.AppointmentCategoryController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentCategoryService
import java.util.Optional

@WebMvcTest(controllers = [AppointmentCategoryController::class])
@ContextConfiguration(classes = [AppointmentCategoryController::class])
class AppointmentCategoryControllerTest : ControllerTestBase<AppointmentCategoryController>() {

  @MockitoBean
  private lateinit var appointmentCategoryRepository: AppointmentCategoryRepository

  @MockitoBean
  private lateinit var appointmentCategoryService: AppointmentCategoryService

  override fun controller() = AppointmentCategoryController(appointmentCategoryService, appointmentCategoryRepository)

  @Test
  fun `200 response when get all appointment categories`() {
    val expectedModel = listOf(
      AppointmentCategorySummary(
        code = "TEST",
        description = "Test Category",
      ),
    )

    whenever(appointmentCategoryService.get()).thenReturn(listOf(appointmentCategorySummary()))

    val response = mockMvc
      .get("/appointment-categories")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))
  }

  @Test
  fun `200 response when get one appointment category`() {
    val expectedModel = expectedAppointmentCategory()
    whenever(appointmentCategoryRepository.findById(1)).thenReturn(Optional.of(appointmentCategory()))

    val response = mockMvc
      .get("/appointment-categories/1")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))
  }

  @Test
  fun `201 response when creating an appointment category`() {
    val createRequest = appointmentCategoryRequest()
    val expectedModel = expectedAppointmentCategory()

    whenever(appointmentCategoryService.create(createRequest)).thenReturn(expectedModel)

    val response = mockMvc
      .post("/appointment-categories") {
        contentType = MediaType.APPLICATION_JSON
        accept = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(createRequest)
      }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isCreated() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))
  }

  @Test
  fun `202 response when updating an appointment category`() {
    val updateRequest = appointmentCategoryRequest()
    val expectedModel = expectedAppointmentCategory()

    whenever(appointmentCategoryService.update(1, updateRequest)).thenReturn(expectedModel)

    val response = mockMvc
      .put("/appointment-categories/1") {
        contentType = MediaType.APPLICATION_JSON
        accept = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(updateRequest)
      }
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isAccepted() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedModel))
  }

  @Test
  fun `200 response when deleting an appointment category`() {
    whenever(appointmentCategoryRepository.findById(1)).thenReturn(Optional.of(appointmentCategory()))

    val response = mockMvc
      .delete("/appointment-categories/1")
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo("")
  }

  private fun appointmentCategoryRequest() = AppointmentCategoryRequest(
    code = "category code",
    description = "category description",
    appointmentParentCategoryId = 5,
    status = CategoryStatus.ACTIVE,
  )

  private fun expectedAppointmentCategory() = AppointmentCategory(
    id = 1,
    code = "category code",
    description = "category description",
    appointmentParentCategory = AppointmentParentCategory(5, "Category"),
    status = CategoryStatus.ACTIVE,
  )
}
