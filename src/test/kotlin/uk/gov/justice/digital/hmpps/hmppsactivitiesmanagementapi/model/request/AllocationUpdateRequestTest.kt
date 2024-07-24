package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource

class AllocationUpdateRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `start date must not be in the past`() {
    val request = AllocationUpdateRequest(
      startDate = TimeSource.yesterday(),
    )
    assertSingleValidationError(validator.validate(request), "startDate", "Start date must not be in the past")
  }

  @Test
  fun `end date must not be in the past`() {
    val request = AllocationUpdateRequest(
      endDate = TimeSource.yesterday(),
    )
    assertSingleValidationError(validator.validate(request), "endDate", "End date must not be in the past")
  }

  private fun assertSingleValidationError(validate: MutableSet<ConstraintViolation<AllocationUpdateRequest>>, propertyName: String, message: String) {
    assertThat(validate.size).isEqualTo(1)
    assertThat(validate.first().propertyPath.toString()).isEqualTo(propertyName)
    assertThat(validate.first().message).isEqualTo(message)
  }
}
