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

  @Test
  fun `Schedule weeks must be greater than 0`() {
    val zeroScheduleWeeks = activityCreateRequest().copy(scheduleWeeks = 0)

    assertThat(
      validator.validate(zeroScheduleWeeks),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("scheduleWeeks")
      assertThat(it.message).isEqualTo("Schedule weeks must be either 1 or 2")
    }
  }

  @Test
  fun `Schedule weeks must be less than or equal to 2`() {
    val threeScheduleWeeks = activityCreateRequest().copy(scheduleWeeks = 3)

    assertThat(
      validator.validate(threeScheduleWeeks),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("scheduleWeeks")
      assertThat(it.message).isEqualTo("Schedule weeks must be either 1 or 2")
    }
  }

  @Test
  fun `Unpaid activity cannot have pay rates`() {
    val unpaidActivity = activityCreateRequest(paid = false).copy(pay = listOf(ActivityPayCreateRequest(incentiveLevel = "1", incentiveNomisCode = "2", payBandId = 1)))

    assertThat(
      validator.validate(unpaidActivity),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("unpaid")
      assertThat(it.message).isEqualTo("Unpaid activity cannot have pay rates associated with it")
    }
  }
}
