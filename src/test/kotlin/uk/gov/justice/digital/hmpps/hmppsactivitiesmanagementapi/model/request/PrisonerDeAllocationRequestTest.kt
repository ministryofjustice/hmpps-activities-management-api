package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource

class PrisonerDeAllocationRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `end date must not be in the past`() {
    val request = PrisonerDeallocationRequest(
      prisonerNumbers = listOf("G4793VF"),
      endDate = TimeSource.yesterday(),
      reasonCode = "OTHER",
      caseNote = null,
    )
    assertSingleValidationError(validator.validate(request), "endDate", "End date must not be in the past")
  }

  private fun assertSingleValidationError(validate: MutableSet<ConstraintViolation<PrisonerDeallocationRequest>>, propertyName: String, message: String) {
    assertThat(validate.size).isEqualTo(1)
    assertThat(validate.first().propertyPath.toString()).isEqualTo(propertyName)
    assertThat(validate.first().message).isEqualTo(message)
  }
}
