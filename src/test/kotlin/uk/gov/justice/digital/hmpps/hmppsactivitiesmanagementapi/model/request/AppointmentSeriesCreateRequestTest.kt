package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentSeriesSchedule
import java.time.LocalDate
import java.time.LocalTime

class AppointmentSeriesCreateRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid request`() {
    val request = appointmentSeriesCreateRequest()
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `category code must be supplied`() {
    val request = appointmentSeriesCreateRequest(categoryCode = null)
    assertSingleValidationError(validator.validate(request), "categoryCode", "Category code must be supplied")
  }

  @Test
  fun `prison code must be supplied`() {
    val request = appointmentSeriesCreateRequest(prisonCode = null)
    assertSingleValidationError(validator.validate(request), "prisonCode", "Prison code must be supplied")
  }

  @Test
  fun `prison code should not exceed 3 characters`() {
    val request = appointmentSeriesCreateRequest(prisonCode = "TEST")
    assertSingleValidationError(validator.validate(request), "prisonCode", "Prison code should not exceed 3 characters")
  }

  @Test
  fun `internal location id must be supplied if in cell = false`() {
    val request = appointmentSeriesCreateRequest(internalLocationId = null, inCell = false)
    assertSingleValidationError(validator.validate(request), "internalLocationId", "Internal location id must be supplied if in cell = false")
  }

  @Test
  fun `start date must be supplied`() {
    val request = appointmentSeriesCreateRequest(startDate = null)
    assertSingleValidationError(validator.validate(request), "startDate", "Start date must be supplied")
  }

  @Test
  fun `start date must not be in the past`() {
    val request = appointmentSeriesCreateRequest(startDate = LocalDate.now().minusDays(1))
    assertSingleValidationError(validator.validate(request), "startDate", "Start date must not be in the past")
  }

  @Test
  fun `start time must be supplied`() {
    val request = appointmentSeriesCreateRequest(startTime = null)
    assertSingleValidationError(validator.validate(request), "startTime", "Start time must be supplied")
  }

  @Test
  fun `start time must be in the future`() {
    val request = appointmentSeriesCreateRequest(startDate = LocalDate.now(), startTime = LocalTime.now().minusMinutes(1), endTime = LocalTime.now().plusHours(1))
    assertSingleValidationError(validator.validate(request), "startTime", "Start time must be in the future")
  }

  @Test
  fun `end time must be supplied`() {
    val request = appointmentSeriesCreateRequest(endTime = null)
    assertSingleValidationError(validator.validate(request), "endTime", "End time must be supplied")
  }

  @Test
  fun `end time must be after the start time`() {
    val request = appointmentSeriesCreateRequest(startTime = LocalTime.of(13, 0), endTime = LocalTime.of(13, 0))
    assertSingleValidationError(validator.validate(request), "endTime", "End time must be after the start time")
  }

  @Test
  fun `at least one prisoner number must be supplied`() {
    val request = appointmentSeriesCreateRequest(prisonerNumbers = listOf())
    assertSingleValidationError(validator.validate(request), "prisonerNumbers", "At least one prisoner number must be supplied")
  }

  @Test
  fun `cannot allocate more than one prisoner to an individual appointment`() {
    val request = appointmentSeriesCreateRequest(appointmentType = AppointmentType.INDIVIDUAL, prisonerNumbers = listOf("A1234BC", "BC2345D"))
    assertSingleValidationError(validator.validate(request), "prisonerNumbers", "Cannot allocate more than one prisoner to an individual appointment")
  }

  @Test
  fun `frequency must be supplied`() {
    val request = appointmentSeriesCreateRequest(schedule = AppointmentSeriesSchedule(frequency = null, numberOfAppointments = 6))
    assertSingleValidationError(validator.validate(request), "schedule.frequency", "Frequency must be supplied")
  }

  @Test
  fun `number of appointments must be supplied`() {
    val request = appointmentSeriesCreateRequest(schedule = AppointmentSeriesSchedule(frequency = AppointmentFrequency.FORTNIGHTLY, numberOfAppointments = null))
    assertSingleValidationError(validator.validate(request), "schedule.numberOfAppointments", "Number of appointments must be supplied")
  }

  @Test
  fun `number of appointments must be greater than 0`() {
    val request = appointmentSeriesCreateRequest(schedule = AppointmentSeriesSchedule(frequency = AppointmentFrequency.MONTHLY, numberOfAppointments = 0))
    assertSingleValidationError(validator.validate(request), "schedule.numberOfAppointments", "Number of appointments must be 1 or greater")
  }

  @Test
  fun `custom name must not be more than 40 characters`() {
    val request = appointmentSeriesCreateRequest(customName = "123456789012345678900123456789012345678901")
    assertSingleValidationError(validator.validate(request), "customName", "Custom name should not exceed 40 characters")
  }

  @Test
  fun `extra information must not be more than 4,000 characters`() {
    val request = appointmentSeriesCreateRequest(extraInformation = "a".repeat(4001))
    assertSingleValidationError(validator.validate(request), "extraInformation", "Extra information must not exceed 4000 characters")
  }

  @Test
  fun `appointment tier code must be supplied`() {
    val request = appointmentSeriesCreateRequest(tierCode = null)
    assertSingleValidationError(
      validator.validate(request),
      "tierCode",
      "Tier code must be supplied",
    )
  }

  private fun assertSingleValidationError(validate: MutableSet<ConstraintViolation<AppointmentSeriesCreateRequest>>, propertyName: String, message: String) {
    assertThat(validate.size).isEqualTo(1)
    assertThat(validate.first().propertyPath.toString()).isEqualTo(propertyName)
    assertThat(validate.first().message).isEqualTo(message)
  }
}
