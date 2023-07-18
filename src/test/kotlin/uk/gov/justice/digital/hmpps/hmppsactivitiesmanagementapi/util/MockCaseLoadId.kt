package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID

fun addCaseLoadIdToRequestHeader(caseLoadId: String) {
  val mockRequest = MockHttpServletRequest()
  mockRequest.addHeader(CASELOAD_ID, caseLoadId)
  RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))
}

fun clearCaseLoadIdFromRequestHeader() {
  val mockRequest = MockHttpServletRequest()
  mockRequest.clearAttributes()
  RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))
}
