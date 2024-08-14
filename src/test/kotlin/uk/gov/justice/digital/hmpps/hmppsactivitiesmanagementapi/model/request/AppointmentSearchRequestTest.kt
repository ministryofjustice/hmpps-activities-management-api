package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AppointmentSearchRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid request`() {
    val request = AppointmentSearchRequest(startDate = LocalDate.now())
    assertThat(validator.validate(request)).isEmpty()
  }
}
