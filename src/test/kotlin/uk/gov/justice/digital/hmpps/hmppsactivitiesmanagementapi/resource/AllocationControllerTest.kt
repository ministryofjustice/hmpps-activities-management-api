package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import java.security.Principal
import java.time.LocalDate

@WebMvcTest(controllers = [AllocationController::class])
@ContextConfiguration(classes = [AllocationController::class])
class AllocationControllerTest : ControllerTestBase<AllocationController>() {

  @MockBean
  private lateinit var allocationsService: AllocationsService

  @MockBean
  private lateinit var waitingListService: WaitingListService

  override fun controller() = AllocationController(allocationsService, waitingListService)

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

    assertThat(response.contentAsString).contains(
      DeallocationReason.COMPLETED.name,
      DeallocationReason.HEALTH.name,
      DeallocationReason.OTHER.name,
      DeallocationReason.SECURITY.name,
      DeallocationReason.TRANSFERRED.name,
      DeallocationReason.WITHDRAWN_OWN.name,
      DeallocationReason.WITHDRAWN_STAFF.name,
    )

    assertThat(response.contentAsString).doesNotContain(
      DeallocationReason.DIED.name,
      DeallocationReason.ENDED.name,
      DeallocationReason.EXPIRED.name,
      DeallocationReason.RELEASED.name,
      DeallocationReason.TEMPORARY_ABSENCE.name,
    )
  }

  @Test
  fun `204 response when add prisoner to activity waiting list`() {
    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = 1L,
      applicationDate = LocalDate.now(),
      requestedBy = "a".repeat(100),
      comments = "a".repeat(500),
      status = WaitingListStatus.PENDING,
    )

    mockMvc.waitingListApplication(pentonvillePrisonCode, request).andExpect { status { isNoContent() } }

    verify(waitingListService).addPrisoner(pentonvillePrisonCode, request, "USERNAME")
  }

  fun createAuthentication(role: String = "ROLE_PRISON"): Authentication {
    val auth = TestingAuthenticationToken("USER", "password", listOf(SimpleGrantedAuthority(role)))
    SecurityContextHolder.getContext().authentication = auth
    return auth
  }

  @Nested
  @DisplayName("Authorization tests")
  inner class AuthorizationTests() {
    @Nested
    @DisplayName("Update allocation")
    inner class UpdateAllocationTests() {
      private val updateAllocation = AllocationUpdateRequest(
        startDate = LocalDate.now().plusDays(1),
      )

      @Test
      @WithMockUser(roles = ["ACTIVITY_ADMIN"])
      fun `Update allocation (ROLE_ACTIVITY_ADMIN) - 202`() {
        mockMvcWithSecurity.patch("/allocations/$pentonvillePrisonCode/allocationId/1") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(updateAllocation)
        }.andExpect { status { isAccepted() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_HUB"])
      fun `Update allocation (ROLE_ACTIVITY_HUB) - 202`() {
        mockMvcWithSecurity.patch("/allocations/$pentonvillePrisonCode/allocationId/1") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(updateAllocation)
        }.andExpect { status { isAccepted() } }
      }

      @Test
      @WithMockUser(roles = ["PRISON"])
      fun `Update allocation (ROLE_PRISON) - 403`() {
        mockMvcWithSecurity.patch("/allocations/$pentonvillePrisonCode/allocationId/1") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(updateAllocation)
        }.andExpect { status { isForbidden() } }
      }
    }

    @Nested
    @DisplayName("Add to waiting list")
    inner class WaitingListTests() {
      private val waitingListRequest = WaitingListApplicationRequest(
        prisonerNumber = "123456",
        activityScheduleId = 1L,
        applicationDate = LocalDate.now(),
        requestedBy = "a".repeat(100),
        comments = "a".repeat(500),
        status = WaitingListStatus.PENDING,
      )

      @Test
      @WithMockUser(roles = ["ACTIVITY_ADMIN"])
      fun `Add to waiting list (ROLE_ACTIVITY_ADMIN) - 204`() {
        mockMvcWithSecurity.post("/allocations/$pentonvillePrisonCode/waiting-list-application") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(waitingListRequest)
        }.andExpect { status { isNoContent() } }
      }

      @Test
      @WithMockUser(roles = ["ACTIVITY_HUB"])
      fun `Add to waiting list (ROLE_ACTIVITY_HUB) - 204`() {
        mockMvcWithSecurity.post("/allocations/$pentonvillePrisonCode/waiting-list-application") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(waitingListRequest)
        }.andExpect { status { isNoContent() } }
      }

      @Test
      @WithMockUser(roles = ["PRISON"])
      fun `Add to waiting list (ROLE_PRISON) - 403`() {
        mockMvcWithSecurity.post("/allocations/$pentonvillePrisonCode/waiting-list-application") {
          contentType = MediaType.APPLICATION_JSON
          content = mapper.writeValueAsBytes(waitingListRequest)
        }.andExpect { status { isForbidden() } }
      }
    }
  }

  private fun MockMvc.waitingListApplication(prisonCode: String, request: WaitingListApplicationRequest) =
    post("/allocations/$prisonCode/waiting-list-application") {
      principal = Principal { "USERNAME" }
      content = mapper.writeValueAsString(request)
      contentType = MediaType.APPLICATION_JSON
    }

  private fun MockMvc.getAllocationById(id: Long) = get("/allocations/id/{allocationId}", id)
}
