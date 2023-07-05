package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCreateRequest

class ActivityCreateRequestTest {

  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid create activity request`() {
    assertThat(validator.validate(activityCreateRequest())).isEmpty()
  }

  @Test
  fun `start date must be in the future`() {
    val activityStartingToday = activityCreateRequest().copy(startDate = TimeSource.today())

    assertThat(
      validator.validate(activityStartingToday),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("startDate")
      assertThat(it.message).isEqualTo("Activity start date must be in the future")
    }
  }
}
