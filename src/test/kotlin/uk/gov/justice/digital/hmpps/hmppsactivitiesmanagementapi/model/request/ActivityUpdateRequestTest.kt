package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource

class ActivityUpdateRequestTest {

  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `start date must be in the future`() {
    val activityStartingToday = ActivityUpdateRequest(startDate = TimeSource.today())

    assertThat(
      validator.validate(activityStartingToday),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("startDate")
      assertThat(it.message).isEqualTo("Activity start date must be in the future")
    }
  }
}
