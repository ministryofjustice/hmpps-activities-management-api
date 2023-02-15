package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCreateRequest
import java.time.LocalDate
import java.time.LocalTime

class AppointmentCreateRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid request`() {
    val request = appointmentCreateRequest()
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `category id must be supplied`() {
    val request = appointmentCreateRequest(categoryId = null)
    assertSingleValidationError(validator.validate(request), "categoryId", "Category id must be supplied")
  }

  @Test
  fun `prison code must be supplied`() {
    val request = appointmentCreateRequest(prisonCode = null)
    assertSingleValidationError(validator.validate(request), "prisonCode", "Prison code must be supplied")
  }

  @Test
  fun `prison code should not exceed 3 characters`() {
    val request = appointmentCreateRequest(prisonCode = "TEST")
    assertSingleValidationError(validator.validate(request), "prisonCode", "Prison code should not exceed 3 characters")
  }

  @Test
  fun `internal location id must be supplied if in cell = false`() {
    val request = appointmentCreateRequest(internalLocationId = null, inCell = false)
    assertSingleValidationError(validator.validate(request), "internalLocationId", "Internal location id must be supplied if in cell = false")
  }

  @Test
  fun `start date must be supplied`() {
    val request = appointmentCreateRequest(startDate = null)
    assertSingleValidationError(validator.validate(request), "startDate", "Start date must be supplied")
  }

  @Test
  fun `start date must not be in the past`() {
    val request = appointmentCreateRequest(startDate = LocalDate.now().minusDays(1))
    assertSingleValidationError(validator.validate(request), "startDate", "Start date must not be in the past")
  }

  @Test
  fun `start time must be supplied`() {
    val request = appointmentCreateRequest(startTime = null)
    assertSingleValidationError(validator.validate(request), "startTime", "Start time must be supplied")
  }

  @Test
  fun `end time must be supplied`() {
    val request = appointmentCreateRequest(endTime = null)
    assertSingleValidationError(validator.validate(request), "endTime", "End time must be supplied")
  }

  @Test
  fun `end time must be after the start time`() {
    val request = appointmentCreateRequest(startTime = LocalTime.of(13, 0), endTime = LocalTime.of(13, 0))
    assertSingleValidationError(validator.validate(request), "endTime", "End time must be after the start time")
  }

  @Test
  fun `at least one prisoner number must be supplied`() {
    val request = appointmentCreateRequest(prisonerNumbers = listOf())
    assertSingleValidationError(validator.validate(request), "prisonerNumbers", "At least one prisoner number must be supplied")
  }

  private fun assertSingleValidationError(validate: MutableSet<ConstraintViolation<AppointmentCreateRequest>>, propertyName: String, message: String) {
    assertThat(validate.size).isEqualTo(1)
    assertThat(validate.first().propertyPath.toString()).isEqualTo(propertyName)
    assertThat(validate.first().message).isEqualTo(message)
  }
}
