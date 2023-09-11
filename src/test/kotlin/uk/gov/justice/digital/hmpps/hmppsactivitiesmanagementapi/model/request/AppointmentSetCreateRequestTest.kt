package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSetCreateRequest
import java.time.LocalDate
import java.time.LocalTime

class AppointmentSetCreateRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid request`() {
    val request = appointmentSetCreateRequest()
    val appointmentSetValidationResult = validator.validate(request)
    assertThat(appointmentSetValidationResult).isEmpty()

    val appointmentValidationResult = validator.validate(request.appointments.first())
    assertThat(appointmentValidationResult).isEmpty()
  }

  @Test
  fun `prison code must be supplied`() {
    val request = appointmentSetCreateRequest(prisonCode = null)
    assertSingleValidationError(validator.validate(request), "prisonCode", "Prison code must be supplied")
  }

  @Test
  fun `prison code should not exceed 3 characters`() {
    val request = appointmentSetCreateRequest(prisonCode = "TEST")
    assertSingleValidationError(validator.validate(request), "prisonCode", "Prison code should not exceed 3 characters")
  }

  @Test
  fun `category code must be supplied`() {
    val request = appointmentSetCreateRequest(categoryCode = null)
    assertSingleValidationError(validator.validate(request), "categoryCode", "Category code must be supplied")
  }

  @Test
  fun `custom name must not be more than 40 characters`() {
    val request = appointmentSetCreateRequest(customName = "123456789012345678900123456789012345678901")
    assertSingleValidationError(validator.validate(request), "customName", "Custom name should not exceed 40 characters")
  }

  @Test
  fun `internal location id must be supplied if in cell = false`() {
    val request = appointmentSetCreateRequest(internalLocationId = null, inCell = false)
    assertSingleValidationError(validator.validate(request), "internalLocationId", "Internal location id must be supplied if in cell = false")
  }

  @Test
  fun `start date must be supplied`() {
    val request = appointmentSetCreateRequest(startDate = null)
    assertSingleValidationError(validator.validate(request), "startDate", "Start date must be supplied")
  }

  @Test
  fun `start date must not be in the past`() {
    val request = appointmentSetCreateRequest(startDate = LocalDate.now().minusDays(1))
    assertSingleValidationError(validator.validate(request), "startDate", "Start date must not be in the past")
  }

  @Test
  fun `request must have at least one appointment`() {
    val requestWithNoAppointments = appointmentSetCreateRequest().copy(appointments = emptyList())

    assertSingleValidationError(
      validator.validate(requestWithNoAppointments),
      "appointments",
      "At least one appointment must be supplied",
    )
  }

  @Test
  fun `prisoner number must be supplied`() {
    val request = appointmentSetCreateRequest(prisonerNumbers = listOf(null))
    assertSingleValidationError(validator.validate(request), "appointments[0].prisonerNumber", "Prisoner number must be supplied")
  }

  @Test
  fun `start time must be supplied`() {
    val request = appointmentSetCreateRequest(startTime = null, prisonerNumbers = listOf("A1234BC"))
    assertSingleValidationError(validator.validate(request), "appointments[0].startTime", "Start time must be supplied")
  }

  @Test
  fun `start times must be in the future`() {
    val request = appointmentSetCreateRequest(
      startDate = LocalDate.now(),
      startTime = LocalTime.now().minusMinutes(1),
      endTime = LocalTime.now().plusHours(1),
    )
    assertSingleValidationError(validator.validate(request), "startTime", "Start times must be in the future")
  }

  @Test
  fun `end time must be supplied`() {
    val request = appointmentSetCreateRequest(endTime = null, prisonerNumbers = listOf("A1234BC"))
    assertSingleValidationError(validator.validate(request), "appointments[0].endTime", "End time must be supplied")
  }

  @Test
  fun `end time must be after the start time`() {
    val request = appointmentSetCreateRequest(
      startTime = LocalTime.of(13, 0),
      endTime = LocalTime.of(13, 0),
      prisonerNumbers = listOf("A1234BC"),
    )
    assertSingleValidationError(validator.validate(request), "appointments[0].endTime", "End time must be after the start time")
  }

  @Test
  fun `appointment extra information must not be more than 4,000 characters`() {
    val request = appointmentSetCreateRequest(extraInformation = "a".repeat(4001), prisonerNumbers = listOf("A1234BC"))
    assertSingleValidationError(
      validator.validate(request),
      "appointments[0].extraInformation",
      "Extra information must not exceed 4000 characters",
    )
  }

  private fun <T> assertSingleValidationError(
    validate: MutableSet<ConstraintViolation<T>>,
    propertyName: String,
    message: String,
  ) {
    assertThat(validate.size).isEqualTo(1)
    assertThat(validate.first().propertyPath.toString()).isEqualTo(propertyName)
    assertThat(validate.first().message).isEqualTo(message)
  }
}
