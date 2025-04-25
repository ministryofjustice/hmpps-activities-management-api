package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ScheduledInstancedUpdateRequestTest : ValidatorBase<ScheduledInstancedUpdateRequest>() {

  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `success if reason is 60 characters`() {
    val request = ScheduledInstancedUpdateRequest(cancelledReason = "X".repeat(60))
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `success if reason is empty`() {
    val request = ScheduledInstancedUpdateRequest(cancelledReason = "")
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `error if reason is 61 characters`() {
    val request = ScheduledInstancedUpdateRequest(cancelledReason = "X".repeat(61))

    request failsWithSingle ModelError("cancelledReason", "Cancellation reason must not exceed 60 characters")
  }

  @Test
  fun `success if comment is 250 characters`() {
    val request = ScheduledInstancedUpdateRequest(cancelledReason = "reason", comment = "X".repeat(250))
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `error if comment is 251 characters`() {
    val request = ScheduledInstancedUpdateRequest(cancelledReason = "reason", comment = "X".repeat(251))

    request failsWithSingle ModelError("comment", "Comment cannot exceed 250 characters")
  }

  @Test
  fun `error if comment is provided without reason`() {
    val request = ScheduledInstancedUpdateRequest(comment = "Comment")

    request failsWithSingle ModelError("commentWithoutReason", "Comment can only be updated if cancelledReason is provided")
  }
}
