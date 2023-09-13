package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import java.time.LocalDate
import java.time.LocalTime

class AppointmentUpdateRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid request`() {
    val request = AppointmentUpdateRequest()
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `internal location id must be supplied if in cell = false`() {
    val request = AppointmentUpdateRequest(internalLocationId = null, inCell = false)
    assertSingleValidationError(validator.validate(request), "internalLocationId", "Internal location id must be supplied if in cell = false")
  }

  @Test
  fun `start date must not be in the past`() {
    val request = AppointmentUpdateRequest(startDate = LocalDate.now().minusDays(1))
    assertSingleValidationError(validator.validate(request), "startDate", "Start date must not be in the past")
  }

  @Test
  fun `start time must be in the future`() {
    val request = AppointmentUpdateRequest(startDate = LocalDate.now(), startTime = LocalTime.now().minusMinutes(1), endTime = LocalTime.now().plusHours(1))
    assertSingleValidationError(validator.validate(request), "startTime", "Start time must be in the future")
  }

  @Test
  fun `end time must be after the start time`() {
    val request = AppointmentUpdateRequest(startTime = LocalTime.of(13, 0), endTime = LocalTime.of(13, 0))
    assertSingleValidationError(validator.validate(request), "endTime", "End time must be after the start time")
  }

  @Test
  fun `cannot update start date for all future appointments`() {
    val request = AppointmentUpdateRequest(startDate = LocalDate.now().plusDays(1), applyTo = ApplyTo.ALL_FUTURE_APPOINTMENTS)
    assertSingleValidationError(validator.validate(request), "applyTo", "Cannot update start date for all future appointments")
  }

  @Test
  fun `is property update false when no property update is supplied`() {
    val request = AppointmentUpdateRequest()
    request.isPropertyUpdate() isEqualTo false
  }

  @Test
  fun `is property update true when category code update is supplied`() {
    val request = AppointmentUpdateRequest(categoryCode = "NEW")
    request.isPropertyUpdate() isEqualTo true
  }

  @Test
  fun `is property update true when internal location update is supplied`() {
    val request = AppointmentUpdateRequest(internalLocationId = 123)
    request.isPropertyUpdate() isEqualTo true
  }

  @Test
  fun `is property update true when in cell update is supplied`() {
    val request = AppointmentUpdateRequest(inCell = true)
    request.isPropertyUpdate() isEqualTo true
  }

  @Test
  fun `is property update true when start date update is supplied`() {
    val request = AppointmentUpdateRequest(startDate = LocalDate.now().plusDays(1))
    request.isPropertyUpdate() isEqualTo true
  }

  @Test
  fun `is property update true when start time update is supplied`() {
    val request = AppointmentUpdateRequest(startTime = LocalTime.of(11, 30))
    request.isPropertyUpdate() isEqualTo true
  }

  @Test
  fun `is property update true when end time update is supplied`() {
    val request = AppointmentUpdateRequest(endTime = LocalTime.of(14, 0))
    request.isPropertyUpdate() isEqualTo true
  }

  @Test
  fun `is property update true when comment update is supplied`() {
    val request = AppointmentUpdateRequest(extraInformation = "New")
    request.isPropertyUpdate() isEqualTo true
  }

  private fun assertSingleValidationError(validate: MutableSet<ConstraintViolation<AppointmentUpdateRequest>>, propertyName: String, message: String) {
    assertThat(validate.size).isEqualTo(1)
    assertThat(validate.first().propertyPath.toString()).isEqualTo(propertyName)
    assertThat(validate.first().message).isEqualTo(message)
  }
}
