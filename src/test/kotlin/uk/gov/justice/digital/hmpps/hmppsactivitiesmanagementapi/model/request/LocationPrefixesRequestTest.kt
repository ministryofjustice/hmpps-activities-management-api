package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LocationPrefixesRequestTest {

  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `should pass validation when list of sub-locations is empty`() {
    val request = LocationPrefixesRequest(emptyList())

    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `should pass validation when list of sub-locations is not empty`() {
    val request = LocationPrefixesRequest(listOf("North Landing 1", "North All"))

    assertThat(validator.validate(request)).isEmpty()
  }
}
