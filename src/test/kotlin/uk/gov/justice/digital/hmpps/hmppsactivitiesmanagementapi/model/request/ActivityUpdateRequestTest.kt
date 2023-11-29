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

  @Test
  fun `Schedule weeks must be greater than 0`() {
    val zeroScheduleWeeks = ActivityUpdateRequest().copy(scheduleWeeks = 0)

    assertThat(
      validator.validate(zeroScheduleWeeks),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("scheduleWeeks")
      assertThat(it.message).isEqualTo("Schedule weeks must be either 1 or 2")
    }
  }

  @Test
  fun `Schedule weeks must be less than or equal to 2`() {
    val threeScheduleWeeks = ActivityUpdateRequest().copy(scheduleWeeks = 3)

    assertThat(
      validator.validate(threeScheduleWeeks),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("scheduleWeeks")
      assertThat(it.message).isEqualTo("Schedule weeks must be either 1 or 2")
    }
  }

  @Test
  fun `Paid activity must have pay rates`() {
    val paidActivityNoPay = ActivityUpdateRequest(paid = true)

    assertThat(
      validator.validate(paidActivityNoPay),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("paid")
      assertThat(it.message).isEqualTo("Paid activity must have at least one pay rate associated with it")
    }
  }
}
