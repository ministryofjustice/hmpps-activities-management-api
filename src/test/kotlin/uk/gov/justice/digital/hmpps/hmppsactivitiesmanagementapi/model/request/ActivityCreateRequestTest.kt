package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityPayCreateRequest
import java.time.LocalDate

class ActivityCreateRequestTest : ValidatorBase<ActivityCreateRequest>() {

  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid create activity request`() {
    val ar = activityCreateRequest()
    assertThat(validator.validate(ar)).isEmpty()
  }

  @Test
  fun `valid create activity request with a pay rate effective from date`() {
    val apr = activityPayCreateRequest(startDate = LocalDate.now().plusDays(2))
    val ar = activityCreateRequest(paid = true).copy(pay = listOf(apr))
    assertThat(validator.validate(ar)).isEmpty()
  }

  @Test
  fun `valid create activity request with a pay rate effective from date of 30 days`() {
    val apr = activityPayCreateRequest(startDate = LocalDate.now().plusDays(30))
    val ar = activityCreateRequest(paid = true).copy(pay = listOf(apr))
    assertThat(validator.validate(ar)).isEmpty()
  }

  @Test
  fun `Valid activity with same incentive level, payband and different effective pay from date`() {
    val apr1 = activityPayCreateRequest(startDate = LocalDate.now().plusDays(5))
    val apr2 = activityPayCreateRequest(startDate = LocalDate.now().plusDays(25))
    val ar = activityCreateRequest(paid = true).copy(pay = listOf(apr1, apr2))

    assertThat(validator.validate(ar)).isEmpty()
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
  fun `Paid activity must have pay rates`() {
    val paidActivityNoPay = activityCreateRequest(paid = true).copy(pay = emptyList())

    assertThat(
      validator.validate(paidActivityNoPay),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("paid")
      assertThat(it.message).isEqualTo("Paid activity must have at least one pay rate associated with it")
    }
  }

  @Test
  fun `Paid activity with an effective pay from date must be a maximum of 30 days in the future`() {
    val apr = activityPayCreateRequest(startDate = LocalDate.now().plusDays(31))
    val ar = activityCreateRequest(paid = true).copy(pay = listOf(apr))

    ar failsWithSingle ModelError("maximumFuturePayDate", "Activity pay rate effective date must not be more than 30 days in the future")
  }

  @Test
  fun `Paid activity with an effective pay from date must be unique`() {
    val apr1 = activityPayCreateRequest(startDate = LocalDate.now().plusDays(25))
    val apr2 = activityPayCreateRequest(startDate = LocalDate.now().plusDays(25))
    val ar = activityCreateRequest(paid = true).copy(pay = listOf(apr1, apr2))

    ar failsWithSingle ModelError("duplicateFuturePayDate", "Activity pay rate effective date must be unique for a given incentive level and pay band")
  }

  @Test
  fun `Unpaid activity cannot have pay rates`() {
    val unpaidActivityWithPay = activityCreateRequest(paid = false).copy(pay = listOf(ActivityPayCreateRequest(incentiveLevel = "1", incentiveNomisCode = "2", payBandId = 1)))

    assertThat(
      validator.validate(unpaidActivityWithPay),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("unpaid")
      assertThat(it.message).isEqualTo("Unpaid activity cannot have pay rates associated with it")
    }
  }

  @Test
  fun `Activity for tier 1 cannot have a required attendance of false`() {
    val activityTierOne = activityCreateRequest().copy(tierCode = "TIER_1", attendanceRequired = false)

    assertThat(
      validator.validate(activityTierOne),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("tierOneOrTierTwoAttendedCheck")
      assertThat(it.message).isEqualTo("Activity with tier code Tier 1 or Tier 2 must be attended")
    }
  }

  @Test
  fun `Activity for tier 2 cannot have a required attendance of false`() {
    val activityTierTwo = activityCreateRequest().copy(tierCode = "TIER_2", attendanceRequired = false)

    assertThat(
      validator.validate(activityTierTwo),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("tierOneOrTierTwoAttendedCheck")
      assertThat(it.message).isEqualTo("Activity with tier code Tier 1 or Tier 2 must be attended")
    }
  }

  @Test
  fun `Unpaid activity for tier foundation can have a required attendance of true`() {
    var activityCreateRequest = activityCreateRequest(paid = false).copy(tierCode = "FOUNDATION", attendanceRequired = true, pay = emptyList())
    assertThat(validator.validate(activityCreateRequest).isEmpty())
  }

  @Test
  fun `Unpaid activity for tier foundation can have a required attendance of false`() {
    var activityCreateRequest = activityCreateRequest(paid = false).copy(tierCode = "FOUNDATION", attendanceRequired = false, pay = emptyList())
    assertThat(validator.validate(activityCreateRequest)).isEmpty()
  }

  @Test
  fun `Activity for tier 1 and tier 2 can have a required attendance of true`() {
    assertThat(validator.validate(activityCreateRequest().copy(tierCode = "TIER_1", attendanceRequired = true))).isEmpty()
    assertThat(validator.validate(activityCreateRequest().copy(tierCode = "TIER_2", attendanceRequired = true))).isEmpty()
  }

  @Test
  fun `Activity for foundation tier and not attended must be unpaid`() {
    val activityTierFoundation = activityCreateRequest().copy(tierCode = "FOUNDATION", attendanceRequired = false, paid = true)

    assertThat(
      validator.validate(activityTierFoundation),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("unpaidNotAttendedFoundation")
      assertThat(it.message).isEqualTo("Activity with tier code Foundation and attendance not required must be unpaid")
    }
  }
}
