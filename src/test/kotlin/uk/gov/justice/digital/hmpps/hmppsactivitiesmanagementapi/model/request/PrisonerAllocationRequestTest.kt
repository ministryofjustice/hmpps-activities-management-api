package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource

class PrisonerAllocationRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `start date must not be in the past`() {
    val request = PrisonerAllocationRequest(
      prisonerNumber = "G4793VF",
      payBandId = 11,
      startDate = TimeSource.yesterday(),
    )
    assertSingleValidationError(validator.validate(request), "startDate", "Start date must not be in the past")
  }

  private fun assertSingleValidationError(validate: MutableSet<ConstraintViolation<PrisonerAllocationRequest>>, propertyName: String, message: String) {
    assertThat(validate.size).isEqualTo(1)
    assertThat(validate.first().propertyPath.toString()).isEqualTo(propertyName)
    assertThat(validate.first().message).isEqualTo(message)
  }
}
