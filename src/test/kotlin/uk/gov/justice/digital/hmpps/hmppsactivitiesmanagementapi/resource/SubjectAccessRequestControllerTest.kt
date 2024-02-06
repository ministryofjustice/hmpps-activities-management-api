package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithAnonymousUser
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.get
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SubjectAccessRequestContent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.SubjectAccessRequestService

@WebMvcTest(controllers = [SubjectAccessRequestController::class])
@ContextConfiguration(classes = [SubjectAccessRequestController::class])
@WithMockUser(roles = ["SAR_DATA_ACCESS"])
class SubjectAccessRequestControllerTest : ControllerTestBase<SubjectAccessRequestController>() {

  @MockBean
  private lateinit var service: SubjectAccessRequestService

  private val content: SubjectAccessRequestContent = mock()

  override fun controller() = SubjectAccessRequestController(service)

  @Test
  fun `should return 200 response when prisoner found and no dates provided`() {
    whenever(service.getContentFor("123456", null, null)) doReturn content

    mockMvcWithSecurity.get("/subject-access-request?prn=123456")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }

    verify(service).getContentFor("123456", null, null)
  }

  @Test
  fun `should return 200 response when prisoner found and from date provided`() {
    whenever(service.getContentFor("123456", TimeSource.today(), null)) doReturn content

    mockMvcWithSecurity.get("/subject-access-request?prn=123456&fromDate=${TimeSource.today()}")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }

    verify(service).getContentFor("123456", TimeSource.today(), null)
  }

  @Test
  fun `should return 200 response when prisoner found and to date provided`() {
    whenever(service.getContentFor("123456", null, TimeSource.today())) doReturn content

    mockMvcWithSecurity.get("/subject-access-request?prn=123456&toDate=${TimeSource.today()}")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isOk() } }

    verify(service).getContentFor("123456", null, TimeSource.today())
  }

  @Test
  fun `should return 204 response when prisoner not found`() {
    whenever(service.getContentFor("UNKNOWN", null, null)) doReturn null

    mockMvcWithSecurity.get("/subject-access-request?prn=UNKNOWN")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNoContent() } }

    verify(service).getContentFor("UNKNOWN", null, null)
  }

  @Test
  fun `should return 204 response when prisoner number empty`() {
    mockMvcWithSecurity.get("/subject-access-request?prn= ")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNoContent() } }

    verifyNoInteractions(service)
  }

  @Test
  fun `should return 204 response when prisoner number null`() {
    mockMvcWithSecurity.get("/subject-access-request")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isNoContent() } }

    verifyNoInteractions(service)
  }

  @Test
  fun `should return 209 response for unsupported search type`() {
    mockMvcWithSecurity.get("/subject-access-request?crn=123456")
      .andExpect { content { contentType(MediaType.APPLICATION_JSON_VALUE) } }
      .andExpect { status { isEqualTo(209) } }

    verifyNoInteractions(service)
  }

  @Test
  @WithAnonymousUser
  fun `should return 401 response for unauthorised`() {
    mockMvcWithSecurity.get("/subject-access-request?prn=123456")
      .andExpect { status { isUnauthorized() } }

    verifyNoInteractions(service)
  }

  @Test
  @WithMockUser(roles = ["PRISON"])
  fun `should return 403 response for missing SAR_DATA_ACCESS role`() {
    mockMvcWithSecurity.get("/subject-access-request?prn=123456")
      .andExpect { status { isForbidden() } }

    verifyNoInteractions(service)
  }
}
