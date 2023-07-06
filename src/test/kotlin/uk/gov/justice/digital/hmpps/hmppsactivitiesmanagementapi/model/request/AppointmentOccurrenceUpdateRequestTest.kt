package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCreateRequest
import java.time.LocalDate
import java.time.LocalTime

class AppointmentOccurrenceUpdateRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid request`() {
    val request = appointmentCreateRequest()
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `internal location id must be supplied if in cell = false`() {
    val request = AppointmentOccurrenceUpdateRequest(internalLocationId = null, inCell = false)
    assertSingleValidationError(validator.validate(request), "internalLocationId", "Internal location id must be supplied if in cell = false")
  }

  @Test
  fun `start date must not be in the past`() {
    val request = AppointmentOccurrenceUpdateRequest(startDate = LocalDate.now().minusDays(1))
    assertSingleValidationError(validator.validate(request), "startDate", "Start date must not be in the past")
  }

  @Test
  fun `start time must be in the future`() {
    val request = AppointmentOccurrenceUpdateRequest(startDate = LocalDate.now(), startTime = LocalTime.now().minusMinutes(1), endTime = LocalTime.now().plusHours(1))
    assertSingleValidationError(validator.validate(request), "startTime", "Start time must be in the future")
  }

  @Test
  fun `end time must be after the start time`() {
    val request = AppointmentOccurrenceUpdateRequest(startTime = LocalTime.of(13, 0), endTime = LocalTime.of(13, 0))
    assertSingleValidationError(validator.validate(request), "endTime", "End time must be after the start time")
  }

  @Test
  fun `cannot update start date for all future occurrences`() {
    val request = AppointmentOccurrenceUpdateRequest(startDate = LocalDate.now().plusDays(1), applyTo = ApplyTo.ALL_FUTURE_OCCURRENCES)
    assertSingleValidationError(validator.validate(request), "applyTo", "Cannot update start date for all future occurrences")
  }

  private fun assertSingleValidationError(validate: MutableSet<ConstraintViolation<AppointmentOccurrenceUpdateRequest>>, propertyName: String, message: String) {
    assertThat(validate.size).isEqualTo(1)
    assertThat(validate.first().propertyPath.toString()).isEqualTo(propertyName)
    assertThat(validate.first().message).isEqualTo(message)
  }
}
