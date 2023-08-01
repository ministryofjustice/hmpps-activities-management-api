package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID

const val DEFAULT_CASELOAD_ID = "PVI"

class FakeCaseLoad(private val caseloadId: String = DEFAULT_CASELOAD_ID) : BeforeEachCallback, AfterEachCallback {

  private val mockRequest = MockHttpServletRequest()

  override fun beforeEach(context: ExtensionContext?) {
    RequestContextHolder.setRequestAttributes(
      ServletRequestAttributes(
        mockRequest.apply {
          addHeader(
            CASELOAD_ID,
            caseloadId,
          )
        },
      ),
    )
  }

  override fun afterEach(context: ExtensionContext?) {
    RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest.apply { clearAttributes() }))
  }
}
