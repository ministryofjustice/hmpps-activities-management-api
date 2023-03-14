package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class AppointmentRepeatPeriodTest {
  @Test
  fun `weekday next date increments by one day`() {
    val monday = LocalDate.of(2023, 3, 13)
    val nextDate = AppointmentRepeatPeriod.WEEKDAY.nextDate(monday)
    Assertions.assertThat(nextDate.dayOfMonth).isEqualTo(14)
    Assertions.assertThat(nextDate.dayOfWeek).isEqualTo(DayOfWeek.TUESDAY)
  }

  @Test
  fun `weekday next date increments friday to monday`() {
    val friday = LocalDate.of(2023, 3, 17)
    val nextDate = AppointmentRepeatPeriod.WEEKDAY.nextDate(friday)
    Assertions.assertThat(nextDate.dayOfMonth).isEqualTo(20)
    Assertions.assertThat(nextDate.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
  }

  @Test
  fun `daily next date increments by one day`() {
    val friday = LocalDate.of(2023, 3, 17)
    val nextDate = AppointmentRepeatPeriod.DAILY.nextDate(friday)
    Assertions.assertThat(nextDate.dayOfMonth).isEqualTo(18)
    Assertions.assertThat(nextDate.dayOfWeek).isEqualTo(DayOfWeek.SATURDAY)
  }

  @Test
  fun `weekly next date increments by one week`() {
    val monday = LocalDate.of(2023, 3, 13)
    val nextDate = AppointmentRepeatPeriod.WEEKLY.nextDate(monday)
    Assertions.assertThat(nextDate.dayOfMonth).isEqualTo(20)
    Assertions.assertThat(nextDate.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
  }

  @Test
  fun `fortnightly next date increments by two weeks`() {
    val monday = LocalDate.of(2023, 3, 13)
    val nextDate = AppointmentRepeatPeriod.FORTNIGHTLY.nextDate(monday)
    Assertions.assertThat(nextDate.dayOfMonth).isEqualTo(27)
    Assertions.assertThat(nextDate.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
  }

  @Test
  fun `monthly next date increments by two weeks`() {
    val monday = LocalDate.of(2023, 3, 13)
    val nextDate = AppointmentRepeatPeriod.MONTHLY.nextDate(monday)
    Assertions.assertThat(nextDate.dayOfMonth).isEqualTo(13)
    Assertions.assertThat(nextDate.dayOfWeek).isEqualTo(DayOfWeek.THURSDAY)
  }
}
