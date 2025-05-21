package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AdvanceAttendanceUpdateRequestTest : ValidatorBase<AdvanceAttendanceUpdateRequest>() {

  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid update request`() {
    val request = AdvanceAttendanceUpdateRequest(
      issuePayment = false,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `issue payment must not be null`() {
    val request = AdvanceAttendanceUpdateRequest(
      issuePayment = null,
    )
    assertThat(
      validator.validate(request),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("issuePayment")
      assertThat(it.message).isEqualTo("Issue payment must be supplied")
    }
  }
}
