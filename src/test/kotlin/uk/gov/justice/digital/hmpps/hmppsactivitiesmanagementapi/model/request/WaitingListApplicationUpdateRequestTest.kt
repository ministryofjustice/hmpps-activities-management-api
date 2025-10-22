package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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

  @EnumSource(WaitingListStatus::class, names = ["PENDING", "APPROVED", "DECLINED", "WITHDRAWN"])
  @ParameterizedTest(name = "Status can be {0}")
  fun `status can be`(status: WaitingListStatus) {
    assertNoErrors(request.copy(status = status))
  }

  @EnumSource(WaitingListStatus::class, names = ["PENDING", "APPROVED", "DECLINED", "WITHDRAWN"], mode = EnumSource.Mode.EXCLUDE)
  @ParameterizedTest(name = "Status cannot be {0}")
  fun `status cannot be`(status: WaitingListStatus) {
    assertSingleValidationError(
      request.copy(status = status),
      "status",
      "Only PENDING, APPROVED, DECLINED or WITHDRAWN are allowed for status",
    )
  }
}
