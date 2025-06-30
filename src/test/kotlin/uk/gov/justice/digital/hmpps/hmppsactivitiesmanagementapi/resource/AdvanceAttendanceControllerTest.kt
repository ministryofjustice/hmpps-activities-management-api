package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.advanceAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AdvanceAttendanceCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AdvanceAttendanceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AdvanceAttendanceService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform

@WebMvcTest(controllers = [AdvanceAttendanceController::class])
@ContextConfiguration(classes = [AdvanceAttendanceController::class])
class AdvanceAttendanceControllerTest : ControllerTestBase<AdvanceAttendanceController>() {

  @MockitoBean
  private lateinit var advanceAttendanceService: AdvanceAttendanceService

  override fun controller() = AdvanceAttendanceController(advanceAttendanceService)

  @Test
  fun `getAttendanceById - 200 - when advance attendance exists`() {
    val advanceAttendance = transform(advanceAttendance())

    whenever(advanceAttendanceService.getAttendanceById(advanceAttendance.id)).thenReturn(advanceAttendance)

    val response =
      mockMvc.get("/advance-attendances/${advanceAttendance.id}") {
        principal = user
        accept = MediaType.APPLICATION_JSON
      }
        .andDo { print() }
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isOk() } }
        .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(advanceAttendance))

    verify(advanceAttendanceService).getAttendanceById(advanceAttendance.id)
  }

  @Test
  fun `getAttendanceById -  400 - when advance attendance does not exist`() {
    whenever(advanceAttendanceService.getAttendanceById(333)).thenThrow(EntityNotFoundException("Advance attendance 333 not found"))

    val response =
      mockMvc.get("/advance-attendances/333") {
        principal = user
        accept = MediaType.APPLICATION_JSON
      }
        .andDo { print() }
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andExpect { status { isNotFound() } }
        .andReturn().response

    assertThat(response.contentAsString).contains("Advance attendance 333 not found")
  }

  @Test
  fun `create - 201 - when advance attendance is created`() {
    val request = AdvanceAttendanceCreateRequest(
      scheduleInstanceId = 1,
      prisonerNumber = "A1111AA",
      issuePayment = false,
    )

    val advanceAttendance = transform(advanceAttendance())

    whenever(advanceAttendanceService.create(request, user.name)).thenReturn(advanceAttendance)

    val response =
      mockMvc.post("/advance-attendances") {
        principal = user
        contentType = MediaType.APPLICATION_JSON
        accept = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(request)
      }
        .andDo { print() }
        .andExpect { status { isCreated() } }
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(advanceAttendance))
  }

  @Test
  fun `update - 204 - when advance attendance is updated`() {
    val request = AdvanceAttendanceUpdateRequest(
      issuePayment = false,
    )

    val advanceAttendance = transform(advanceAttendance())

    whenever(advanceAttendanceService.update(1, false, user.name)).thenReturn(advanceAttendance)

    val response =
      mockMvc.put("/advance-attendances/1") {
        principal = user
        contentType = MediaType.APPLICATION_JSON
        accept = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(request)
      }
        .andDo { print() }
        .andExpect { status { isAccepted() } }
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(advanceAttendance))
  }

  @Test
  fun `update - 400 - when advance attendance does not exist`() {
    val request = AdvanceAttendanceUpdateRequest(
      issuePayment = false,
    )

    whenever(advanceAttendanceService.update(333, false, user.name)).thenThrow(EntityNotFoundException("Advance attendance 333 not found"))

    val response =
      mockMvc.put("/advance-attendances/333") {
        principal = user
        contentType = MediaType.APPLICATION_JSON
        accept = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(request)
      }
        .andDo { print() }
        .andExpect { status { isNotFound() } }
        .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
        .andReturn().response

    assertThat(response.contentAsString).contains("Advance attendance 333 not found")
  }

  @Test
  fun `delete - 204 - when advance attendance is update`() {
    mockMvc.delete("/advance-attendances/123") {
      principal = user
      contentType = MediaType.APPLICATION_JSON
    }
      .andDo { print() }
      .andExpect { status { isOk() } }
      .andReturn().response

    verify(advanceAttendanceService).delete(123)
  }

  @Test
  fun `delete - 400 - when advance attendance does not exist`() {
    whenever(advanceAttendanceService.delete(333)).thenThrow(EntityNotFoundException("Advance attendance 333 not found"))

    val response = mockMvc.delete("/advance-attendances/333") {
      principal = user
      contentType = MediaType.APPLICATION_JSON
    }
      .andDo { print() }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Advance attendance 333 not found")
  }

  @Nested
  @DisplayName("Authorization tests")
  inner class AuthorizationTests {
    @Test
    @WithMockUser(roles = ["ACTIVITY_HUB"])
    fun `Get advance attendance (ACTIVITY_HUB) - 403`() {
      mockMvcWithSecurity.get("/advance-attendances/1") {
      }.andExpect { status { isForbidden() } }
    }

    @Test
    @WithMockUser(roles = ["ACTIVITY_HUB"])
    fun `Create advance attendance (ACTIVITY_HUB) - 403`() {
      val request = AdvanceAttendanceCreateRequest(
        scheduleInstanceId = 1,
        prisonerNumber = "A1111AA",
        issuePayment = false,
      )
      mockMvcWithSecurity.post("/advance-attendances") {
        principal = user
        contentType = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(request)
      }.andExpect { status { isForbidden() } }
    }

    @Test
    @WithMockUser(roles = ["ACTIVITY_HUB"])
    fun `Update advance attendance (ACTIVITY_HUB) - 403`() {
      val request = AdvanceAttendanceUpdateRequest(
        issuePayment = false,
      )
      mockMvcWithSecurity.put("/advance-attendances/1") {
        principal = user
        contentType = MediaType.APPLICATION_JSON
        content = mapper.writeValueAsBytes(request)
      }.andExpect { status { isForbidden() } }
    }

    @Test
    @WithMockUser(roles = ["ACTIVITY_HUB"])
    fun `Delete advance attendance (ACTIVITY_HUB) - 403`() {
      mockMvcWithSecurity.delete("/advance-attendances/1") {
        principal = user
      }.andExpect { status { isForbidden() } }
    }
  }
}
