package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentMigrateRequest

class AppointmentMigrateRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid request`() {
    val request = appointmentMigrateRequest()
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `prison code must be supplied`() {
    val request = appointmentMigrateRequest(prisonCode = null)
    assertSingleValidationError(validator.validate(request), "prisonCode", "Prison code must be supplied")
  }

  @Test
  fun `prison code should not exceed 6 characters`() {
    val request = appointmentMigrateRequest(prisonCode = "TEST123")
    assertSingleValidationError(validator.validate(request), "prisonCode", "Prison code should not exceed 6 characters")
  }

  @Test
  fun `prisoner number must be supplied`() {
    val request = appointmentMigrateRequest(prisonerNumber = null)
    assertSingleValidationError(validator.validate(request), "prisonerNumber", "Prisoner number must be supplied")
  }

  @Test
  fun `booking id must be supplied`() {
    val request = appointmentMigrateRequest(bookingId = null)
    assertSingleValidationError(validator.validate(request), "bookingId", "Booking id must be supplied")
  }

  @Test
  fun `category code must be supplied`() {
    val request = appointmentMigrateRequest(categoryCode = null)
    assertSingleValidationError(validator.validate(request), "categoryCode", "Category code must be supplied")
  }

  @Test
  fun `internal location id must be supplied`() {
    val request = appointmentMigrateRequest(internalLocationId = null)
    assertSingleValidationError(validator.validate(request), "internalLocationId", "Internal location id must be supplied")
  }

  @Test
  fun `start date must be supplied`() {
    val request = appointmentMigrateRequest(startDate = null)
    assertSingleValidationError(validator.validate(request), "startDate", "Start date must be supplied")
  }

  @Test
  fun `start time must be supplied`() {
    val request = appointmentMigrateRequest(startTime = null)
    assertSingleValidationError(validator.validate(request), "startTime", "Start time must be supplied")
  }

  @Test
  fun `created must be supplied`() {
    val request = appointmentMigrateRequest(created = null)
    assertSingleValidationError(validator.validate(request), "created", "Created must be supplied")
  }

  @Test
  fun `created by must be supplied`() {
    val request = appointmentMigrateRequest(createdBy = null)
    assertSingleValidationError(validator.validate(request), "createdBy", "Created by must be supplied")
  }

  @Test
  fun `created by should not exceed 100 characters`() {
    val request = appointmentMigrateRequest(createdBy = "A".padEnd(101, 'A'))
    assertSingleValidationError(validator.validate(request), "createdBy", "Created by should not exceed 100 characters")
  }

  @Test
  fun `updated by should not exceed 100 characters`() {
    val request = appointmentMigrateRequest(updatedBy = "A".padEnd(101, 'A'))
    assertSingleValidationError(validator.validate(request), "updatedBy", "Updated by should not exceed 100 characters")
  }

  private fun assertSingleValidationError(validate: MutableSet<ConstraintViolation<AppointmentMigrateRequest>>, propertyName: String, message: String) {
    assertThat(validate.size).isEqualTo(1)
    assertThat(validate.first().propertyPath.toString()).isEqualTo(propertyName)
    assertThat(validate.first().message).isEqualTo(message)
  }
}
