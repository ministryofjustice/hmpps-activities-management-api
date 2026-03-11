package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LocationPrefixesRequestTest {

  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `should fail validation when list of sub-locations is empty`() {
    val request = LocationPrefixesRequest(emptyList())

    validator.validate(request)
      .single()
      .apply {
        assertThat(propertyPath.toString()).isEqualTo("subLocations")
        assertThat(message).isEqualTo("At least one sub-location must be provided")
      }
  }

  @Test
  fun `should pass validation when list of sub-locations is not empty`() {
    val request = LocationPrefixesRequest(listOf("North Landing 1"))

    assertThat(validator.validate(request)).isEmpty()
  }
}
