package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.bulkAppointmentRequest

class BulkAppointmentCreateRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid bulk appointment request`() {
    val request = bulkAppointmentRequest()
    val bulkAppointmentValidationResult = validator.validate(request)
    assertThat(bulkAppointmentValidationResult).isEmpty()

    val individualAppointmentValidationResult = validator.validate(request.appointments.first())
    assertThat(individualAppointmentValidationResult).isEmpty()
  }

  @Test
  fun `appointment comment must not be more than 4,000 characters`() {
    val request = bulkAppointmentRequest(comment = "a".repeat(4001)).appointments.first()
    assertSingleValidationError(validator.validate(request), "comment", "Appointment comment must not exceed 4000 characters")
  }

  private fun assertSingleValidationError(validate: MutableSet<ConstraintViolation<IndividualAppointment>>, propertyName: String, message: String) {
    assertThat(validate.size).isEqualTo(1)
    assertThat(validate.first().propertyPath.toString()).isEqualTo(propertyName)
    assertThat(validate.first().message).isEqualTo(message)
  }
}
