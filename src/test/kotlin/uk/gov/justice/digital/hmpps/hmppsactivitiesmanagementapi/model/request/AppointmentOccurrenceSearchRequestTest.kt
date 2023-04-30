package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AppointmentOccurrenceSearchRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid request`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = LocalDate.now())
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `start date must be supplied`() {
    val request = AppointmentOccurrenceSearchRequest(startDate = null)
    val result = validator.validate(request)
    assertThat(result.size).isEqualTo(1)
    assertThat(result.first().propertyPath.toString()).isEqualTo("startDate")
    assertThat(result.first().message).isEqualTo("Start date must be supplied")
  }
}
