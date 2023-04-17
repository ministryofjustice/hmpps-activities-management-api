package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeat
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentRepeatPeriod
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
  fun `category code must be supplied`() {
    val request = appointmentCreateRequest(categoryCode = null)
    assertSingleValidationError(validator.validate(request), "categoryCode", "Category code must be supplied")
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
  fun `start time must be in the future`() {
    val request = appointmentCreateRequest(startDate = LocalDate.now(), startTime = LocalTime.now().minusMinutes(1), endTime = LocalTime.now().plusHours(1))
    assertSingleValidationError(validator.validate(request), "startTime", "Start time must be in the future")
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

  @Test
  fun `cannot allocate more than one prisoner to an individual appointment`() {
    val request = appointmentCreateRequest(appointmentType = AppointmentType.INDIVIDUAL, prisonerNumbers = listOf("A1234BC", "BC2345D"))
    assertSingleValidationError(validator.validate(request), "prisonerNumbers", "Cannot allocate more than one prisoner to an individual appointment")
  }

  @Test
  fun `repeat period must be supplied`() {
    val request = appointmentCreateRequest(repeat = AppointmentRepeat(period = null, count = 6))
    assertSingleValidationError(validator.validate(request), "repeat.period", "Repeat period must be supplied")
  }

  @Test
  fun `repeat count must be supplied`() {
    val request = appointmentCreateRequest(repeat = AppointmentRepeat(period = AppointmentRepeatPeriod.FORTNIGHTLY, count = null))
    assertSingleValidationError(validator.validate(request), "repeat.count", "Repeat count must be supplied")
  }

  @Test
  fun `repeat count must be greater than 0`() {
    val request = appointmentCreateRequest(repeat = AppointmentRepeat(period = AppointmentRepeatPeriod.MONTHLY, count = 0))
    assertSingleValidationError(validator.validate(request), "repeat.count", "Repeat count must be 1 or greater")
  }

  private fun assertSingleValidationError(validate: MutableSet<ConstraintViolation<AppointmentCreateRequest>>, propertyName: String, message: String) {
    assertThat(validate.size).isEqualTo(1)
    assertThat(validate.first().propertyPath.toString()).isEqualTo(propertyName)
    assertThat(validate.first().message).isEqualTo(message)
  }

  @Test
  fun `appointment description must not be more than 40 characters`() {
    val request = appointmentCreateRequest(appointmentDescription = "123456789012345678900123456789012345678901")
    assertSingleValidationError(validator.validate(request), "appointmentDescription", "Appointment description should not exceed 40 characters")
  }
}
