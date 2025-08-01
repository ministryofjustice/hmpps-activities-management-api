package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTierType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.RISLEY_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentAttendanceSummaryModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchResultModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentSearchRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUncancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.MultipleAppointmentAttendanceRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.appointment.AppointmentController
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentAttendanceService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentSearchService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AttendanceAction
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AttendanceStatus
import java.security.Principal
import java.time.LocalDate
import java.time.LocalTime

@WebMvcTest(controllers = [AppointmentController::class])
@ContextConfiguration(classes = [AppointmentController::class])
class AppointmentControllerTest : ControllerTestBase<AppointmentController>() {
  @MockitoBean
  private lateinit var appointmentService: AppointmentService

  @MockitoBean
  private lateinit var appointmentAttendanceService: AppointmentAttendanceService

  @MockitoBean
  private lateinit var appointmentSearchService: AppointmentSearchService

  override fun controller() = AppointmentController(
    appointmentService,
    appointmentAttendanceService,
    appointmentSearchService,
  )

  @Test
  fun `200 response when get appointment details by valid id`() {
    val appointmentDetails = appointmentDetails()

    whenever(appointmentService.getAppointmentDetailsById(1)).thenReturn(appointmentDetails)

    val response = mockMvc.getAppointmentDetailsById(1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(appointmentDetails))

    verify(appointmentService).getAppointmentDetailsById(1)
  }

  @Test
  fun `404 response when get appointment details by invalid id`() {
    whenever(appointmentService.getAppointmentDetailsById(-1)).thenThrow(EntityNotFoundException("Appointment -1 not found"))

    val response = mockMvc.getAppointmentDetailsById(-1)
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Appointment -1 not found")

    verify(appointmentService).getAppointmentDetailsById(-1)
  }

  @Test
  fun `404 not found response when update appointment by invalid id`() {
    val request = AppointmentUpdateRequest()
    val mockPrincipal: Principal = mock()

    whenever(appointmentService.updateAppointment(-1, request, mockPrincipal)).thenThrow(EntityNotFoundException("Appointment -1 not found"))

    val response = mockMvc.updateAppointment(-1, request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNotFound() } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Appointment -1 not found")

    verify(appointmentService).updateAppointment(-1, request, mockPrincipal)
  }

  @Test
  fun `400 bad request response when update appointment with invalid json`() {
    val request = AppointmentUpdateRequest(
      inCell = false,
      startDate = LocalDate.now().minusDays(1),
      startTime = LocalTime.of(10, 30),
      endTime = LocalTime.of(10, 0),
      addPrisonerNumbers = emptyList(),
    )
    val mockPrincipal: Principal = mock()

    mockMvc.updateAppointment(1, request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect {
        status { isBadRequest() }
        content {
          contentType(MediaType.APPLICATION_JSON)
          jsonPath("$.developerMessage") {
            value(Matchers.containsString("Internal location id or DPS location id must be supplied if in cell = false"))
            value(Matchers.containsString("Start date must not be in the past"))
            value(Matchers.containsString("End time must be after the start time"))
          }
        }
      }

    verifyNoInteractions(appointmentService)
  }

  @Test
  fun `202 accepted response when update appointment with valid json`() {
    val request = AppointmentUpdateRequest()
    val expectedResponse = appointmentSeriesEntity().toModel()

    val mockPrincipal: Principal = mock()

    whenever(appointmentService.updateAppointment(1, request, mockPrincipal)).thenReturn(expectedResponse)

    val response = mockMvc.updateAppointment(1, request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isAccepted() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))
  }

  @Test
  fun `202 response when cancel appointment is called`() {
    val request = AppointmentCancelRequest(
      cancellationReasonId = 1,
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )
    val mockPrincipal: Principal = mock()

    mockMvc.cancelAppointment(1, request, mockPrincipal)
      .andExpect { status { isAccepted() } }
      .andReturn().response
  }

  @Test
  fun `404 not found response when cancel appointment is called with invalid id`() {
    val request = AppointmentCancelRequest(cancellationReasonId = 1)
    val mockPrincipal: Principal = mock()

    whenever(appointmentService.cancelAppointment(-1, request, mockPrincipal)).thenThrow(EntityNotFoundException("Appointment -1 not found"))

    val response = mockMvc.cancelAppointment(-1, request, mockPrincipal)
      .andDo { print() }
      .andExpect { status { isNotFound() } }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Appointment -1 not found")

    verify(appointmentService).cancelAppointment(-1, request, mockPrincipal)
  }

  @Test
  fun `202 response when un-cancel appointment is called`() {
    val request = AppointmentUncancelRequest(
      applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS,
    )
    val mockPrincipal: Principal = mock()

    mockMvc.uncancelAppointment(1, request, mockPrincipal)
      .andExpect { status { isAccepted() } }
      .andReturn().response
  }

  @Test
  fun `404 not found response when un-cancel appointment is called with invalid id`() {
    val request = AppointmentUncancelRequest()
    val mockPrincipal: Principal = mock()

    whenever(appointmentService.uncancelAppointment(-1, request, mockPrincipal)).thenThrow(EntityNotFoundException("Appointment -1 not found"))

    val response = mockMvc.uncancelAppointment(-1, request, mockPrincipal)
      .andDo { print() }
      .andExpect { status { isNotFound() } }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andReturn().response

    assertThat(response.contentAsString).contains("Appointment -1 not found")

    verify(appointmentService).uncancelAppointment(-1, request, mockPrincipal)
  }

  @Test
  fun `200 response when appointment attendance summaries found`() {
    val date = LocalDate.now()
    val mockPrincipal: Principal = mock()

    val summaries = listOf(appointmentAttendanceSummaryModel())

    whenever(appointmentAttendanceService.getAppointmentAttendanceSummaries(RISLEY_PRISON_CODE, date)).thenReturn(summaries)

    val response = mockMvc.getAppointmentAttendanceSummaries(RISLEY_PRISON_CODE, date, mockPrincipal)
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(summaries))
  }

  @Test
  fun `400 response when no date supplied for get appointment attendance summaries`() {
    mockMvc.get("/appointments/$RISLEY_PRISON_CODE/attendance-summaries")
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Required request parameter 'date' for method parameter type LocalDate is not present")
          }
        }
      }

    verifyNoInteractions(appointmentAttendanceService)
  }

  @Test
  fun `400 response when invalid date supplied for get appointment attendance summaries`() {
    mockMvc.get("/appointments/$RISLEY_PRISON_CODE/attendance-summaries?date=invalid")
      .andExpect { status { isBadRequest() } }
      .andExpect {
        content {
          jsonPath("$.userMessage") {
            value("Error converting 'date' (invalid): Method parameter 'date': Failed to convert value of type 'java.lang.String' to required type 'java.time.LocalDate'")
          }
        }
      }

    verifyNoInteractions(appointmentAttendanceService)
  }

  @Test
  fun `500 response when service throws exception for get appointment attendance summaries`() {
    val date = LocalDate.now()
    val mockPrincipal: Principal = mock()

    whenever(appointmentAttendanceService.getAppointmentAttendanceSummaries(RISLEY_PRISON_CODE, date)).thenThrow(RuntimeException("Error"))

    val response = mockMvc.getAppointmentAttendanceSummaries(RISLEY_PRISON_CODE, date, mockPrincipal)
      .andExpect { status { isInternalServerError() } }
      .andReturn().response

    val result = this::class.java.getResource("/__files/error-500.json")?.readText()
    assertThat(response.contentAsString + "\n").isEqualTo(result)
  }

  @Test
  fun `400 response when empty attendance requests marking multiple appointments`() {
    val request = emptyList<MultipleAppointmentAttendanceRequest>()

    val mockPrincipal: Principal = mock()

    mockMvc.updateAttendances(request, AttendanceAction.ATTENDED, mockPrincipal)
      .andDo { print() }
      .andExpect { status { isBadRequest() } }

    verifyNoInteractions(appointmentAttendanceService)
  }

  @Test
  fun `400 response when missing appointment id marking multiple appointments`() {
    val request = listOf(MultipleAppointmentAttendanceRequest(null, listOf("AA1111A")))

    val mockPrincipal: Principal = mock()

    mockMvc.updateAttendances(request, AttendanceAction.ATTENDED, mockPrincipal)
      .andDo { print() }
      .andExpect { status { isBadRequest() } }

    verifyNoInteractions(appointmentAttendanceService)
  }

  @Test
  fun `400 response when action is missing marking multiple appointments`() {
    val request = listOf(MultipleAppointmentAttendanceRequest(1, listOf("AA11111A")))

    val mockPrincipal: Principal = mock()

    mockMvc.updateAttendances(request, null, mockPrincipal)
      .andDo { print() }
      .andExpect { status { isBadRequest() } }

    verifyNoInteractions(appointmentAttendanceService)
  }

  @Test
  fun `400 response when missing prisoner numbers marking multiple appointments`() {
    val request = listOf(MultipleAppointmentAttendanceRequest(1, emptyList()))

    val mockPrincipal: Principal = mock()

    mockMvc.updateAttendances(request, AttendanceAction.ATTENDED, mockPrincipal)
      .andDo { print() }
      .andExpect { status { isBadRequest() } }

    verifyNoInteractions(appointmentAttendanceService)
  }

  @Test
  fun `204 accepted response when marking multiple appointments`() {
    val request = listOf(MultipleAppointmentAttendanceRequest(1, listOf("AA11111A")))

    val mockPrincipal: Principal = mock()

    mockMvc.updateAttendances(request, AttendanceAction.ATTENDED, mockPrincipal)
      .andDo { print() }
      .andExpect { status { isNoContent() } }

    verify(appointmentAttendanceService).markMultipleAttendances(request, AttendanceAction.ATTENDED, mockPrincipal)
  }

  @Test
  fun `202 accepted response when search appointments with valid json`() {
    val request = AppointmentSearchRequest(startDate = LocalDate.now())
    val expectedResponse = listOf(appointmentSearchResultModel())

    val mockPrincipal: Principal = mock()

    whenever(appointmentSearchService.searchAppointments("TPR", request, mockPrincipal)).thenReturn(expectedResponse)

    val response = mockMvc.searchAppointments("TPR", request, mockPrincipal)
      .andDo { print() }
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isAccepted() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(expectedResponse))
  }

  @Nested
  inner class AppointmentAttendanceByStatus {

    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_PRISON"])
    @Test
    fun `verify calls service with all filters`() {
      mockMvc.perform(
        MockMvcRequestBuilders.get("/appointments/MDI/${AttendanceStatus.ATTENDED}/attendance?date=${LocalDate.now()}&customName=custom&prisonerNumber=AE123&eventTier=${EventTierType.TIER_1}&categoryCode=CAT")
          .header("Content-Type", "application/json"),
      ).andExpect(MockMvcResultMatchers.status().isOk)

      verify(appointmentAttendanceService, atLeastOnce()).getAppointmentAttendanceByStatus(
        prisonCode = "MDI",
        status = AttendanceStatus.ATTENDED,
        date = LocalDate.now(),
        categoryCode = "CAT",
        customName = "custom",
        prisonerNumber = "AE123",
        eventTier = EventTierType.TIER_1,
      )
    }

    @WithMockUser(username = "ITAG_USER", authorities = ["ROLE_RANDOM"])
    @Test
    fun `verify security exception with invalid roles`() {
      mockMvcWithSecurity.perform(
        MockMvcRequestBuilders.get("/appointments/MDI/${AttendanceStatus.ATTENDED}/attendance?date=${LocalDate.now()}")
          .header("Content-Type", "application/json"),
      ).andExpect(MockMvcResultMatchers.status().isForbidden)
    }
  }

  @Test
  fun `200 response when get appointments with ids`() {
    val appointments = listOf(appointmentDetails(1), appointmentDetails(2), appointmentDetails(3))

    whenever(appointmentService.getAppointmentDetailsByIds(listOf(1, 2, 3))).thenReturn(appointments)

    val response = mockMvc.getAppointmentDetailsByIds(listOf(1, 2, 3))
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }
      .andReturn().response

    assertThat(response.contentAsString).isEqualTo(mapper.writeValueAsString(appointments))

    verify(appointmentService).getAppointmentDetailsByIds(listOf(1, 2, 3))
  }

  private fun MockMvc.getAppointmentDetailsById(id: Long) = get("/appointments/{appointmentId}/details", id)

  private fun MockMvc.getAppointmentDetailsByIds(ids: List<Long>) = post("/appointments/details") {
    this.principal = principal
    contentType = MediaType.APPLICATION_JSON
    content = mapper.writeValueAsBytes(ids)
  }

  private fun MockMvc.updateAppointment(id: Long, request: AppointmentUpdateRequest, principal: Principal) = patch("/appointments/{appointmentId}", id) {
    this.principal = principal
    contentType = MediaType.APPLICATION_JSON
    content = mapper.writeValueAsBytes(
      request,
    )
  }

  private fun MockMvc.cancelAppointment(id: Long, request: AppointmentCancelRequest, principal: Principal) = put("/appointments/{appointmentId}/cancel", id) {
    this.principal = principal
    contentType = MediaType.APPLICATION_JSON
    content = mapper.writeValueAsBytes(
      request,
    )
  }

  private fun MockMvc.uncancelAppointment(id: Long, request: AppointmentUncancelRequest, principal: Principal) = put("/appointments/{appointmentId}/uncancel", id) {
    this.principal = principal
    contentType = MediaType.APPLICATION_JSON
    content = mapper.writeValueAsBytes(
      request,
    )
  }

  private fun MockMvc.getAppointmentAttendanceSummaries(prisonCode: String, date: LocalDate?, principal: Principal) = get("/appointments/$prisonCode/attendance-summaries?date=$date") {
    this.principal = principal
    accept = MediaType.APPLICATION_JSON
    contentType = MediaType.APPLICATION_JSON
  }.andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }

  private fun MockMvc.updateAttendances(request: List<MultipleAppointmentAttendanceRequest>, action: AttendanceAction?, principal: Principal) = put("/appointments/updateAttendances?action=$action") {
    this.principal = principal
    contentType = MediaType.APPLICATION_JSON
    content = mapper.writeValueAsBytes(
      request,
    )
  }

  private fun MockMvc.searchAppointments(prisonCode: String, request: AppointmentSearchRequest, principal: Principal) = post("/appointments/{prisonCode}/search", prisonCode) {
    this.principal = principal
    contentType = MediaType.APPLICATION_JSON
    content = mapper.writeValueAsBytes(
      request,
    )
  }
}
