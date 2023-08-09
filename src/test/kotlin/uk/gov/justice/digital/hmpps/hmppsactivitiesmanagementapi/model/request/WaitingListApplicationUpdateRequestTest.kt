package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource

class WaitingListApplicationUpdateRequestTest : ValidatorBase<WaitingListApplicationUpdateRequest>() {

  private val request = WaitingListApplicationUpdateRequest()

  @Test
  fun `application date cannot be in the future`() {
    assertSingleValidationError(
      request.copy(applicationDate = TimeSource.tomorrow()),
      "applicationDate",
      "Application date cannot be in the future",
    )
  }

  @Test
  fun `requested by cannot be empty or blank`() {
    assertSingleValidationError(
      request.copy(requestedBy = ""),
      "requestedBy",
      "Requested by cannot be empty or blank",
    )

    assertSingleValidationError(
      request.copy(requestedBy = " "),
      "requestedBy",
      "Requested by cannot be empty or blank",
    )
  }

  @Test
  fun `status can only be pending, approved or declined`() {
    assertSingleValidationError(
      request.copy(status = WaitingListStatus.ALLOCATED),
      "status",
      "Only PENDING, APPROVED or DECLINED are allowed for status",
    )

    assertSingleValidationError(
      request.copy(status = WaitingListStatus.REMOVED),
      "status",
      "Only PENDING, APPROVED or DECLINED are allowed for status",
    )

    assertNoErrors(request.copy(status = WaitingListStatus.PENDING), "status")
    assertNoErrors(request.copy(status = WaitingListStatus.APPROVED), "status")
    assertNoErrors(request.copy(status = WaitingListStatus.DECLINED), "status")
  }
}
