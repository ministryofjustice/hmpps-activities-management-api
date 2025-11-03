package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import java.time.LocalDate

class PrisonerWaitingListApplicationRequestTest : ValidatorBase<PrisonerWaitingListApplicationRequest>() {

  val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  private val request = PrisonerWaitingListApplicationRequest(
    activityScheduleId = 1L,
    applicationDate = LocalDate.now(),
    requestedBy = "a".repeat(100),
    comments = "a".repeat(500),
    status = WaitingListStatus.PENDING,
  )

  @Test
  fun `valid request to add prisoner to an activity`() {
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `activity schedule id must be supplied`() {
    assertSingleValidationError(
      request.copy(activityScheduleId = null),
      "activityScheduleId",
      "Activity schedule identifier must be supplied",
    )
  }

  @Test
  fun `application date must be supplied`() {
    assertSingleValidationError(
      request.copy(applicationDate = null),
      "applicationDate",
      "Application date must be supplied",
    )
  }

  @Test
  fun `application date cannot be in the future`() {
    assertSingleValidationError(
      request.copy(applicationDate = LocalDate.now().plusDays(1)),
      "applicationDate",
      "Application date cannot be in the future",
    )
  }

  @Test
  fun `requested by must be supplied`() {
    assertSingleValidationError(
      request.copy(requestedBy = null),
      "requestedBy",
      "Requested by must be supplied",
    )

    assertSingleValidationError(
      request.copy(requestedBy = ""),
      "requestedBy",
      "Requested by must be supplied",
    )
  }

  @Test
  fun `requested by must not exceed 100 characters`() {
    assertSingleValidationError(
      request.copy(requestedBy = "a".repeat(101)),
      "requestedBy",
      "Requested by must not exceed 100 characters",
    )
  }

  @Test
  fun `comments must not exceed 500 characters`() {
    assertSingleValidationError(
      request.copy(comments = "a".repeat(501)),
      "comments",
      "Comments must not exceed 500 characters",
    )
  }

  @Test
  fun `status must be supplied`() {
    assertSingleValidationError(
      request.copy(status = null),
      "status",
      "Status must be supplied",
    )
  }

  @Test
  fun `status must only be pending, approved or declined`() {
    assertSingleValidationError(
      request.copy(status = WaitingListStatus.REMOVED),
      "status",
      "Only PENDING, APPROVED or DECLINED are allowed for status",
    )

    assertSingleValidationError(
      request.copy(status = WaitingListStatus.WITHDRAWN),
      "status",
      "Only PENDING, APPROVED or DECLINED are allowed for status",
    )

    assertSingleValidationError(
      request.copy(status = WaitingListStatus.ALLOCATED),
      "status",
      "Only PENDING, APPROVED or DECLINED are allowed for status",
    )

    assertNoErrors(request.copy(status = WaitingListStatus.PENDING))
    assertNoErrors(request.copy(status = WaitingListStatus.APPROVED))
    assertNoErrors(request.copy(status = WaitingListStatus.DECLINED))
  }
}
