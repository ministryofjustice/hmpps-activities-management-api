package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate

class AppointmentRepeatPeriodTest {
  private val monday: LocalDate = LocalDate.of(2023, 3, 13)
  private val friday: LocalDate = LocalDate.of(2023, 3, 17)

  @Test
  fun `weekday next date increments by one day`() {
    val nextDate = AppointmentRepeatPeriod.WEEKDAY.occurrenceDate(monday, 2)
    assertThat(nextDate.dayOfMonth).isEqualTo(14)
    assertThat(nextDate.dayOfWeek).isEqualTo(DayOfWeek.TUESDAY)
  }

  @Test
  fun `weekday next date increments friday to monday`() {
    val nextDate = AppointmentRepeatPeriod.WEEKDAY.occurrenceDate(friday, 2)
    assertThat(nextDate.dayOfMonth).isEqualTo(20)
    assertThat(nextDate.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
  }

  @Test
  fun `daily next date increments by one day`() {
    val nextDate = AppointmentRepeatPeriod.DAILY.occurrenceDate(friday, 2)
    assertThat(nextDate.dayOfMonth).isEqualTo(18)
    assertThat(nextDate.dayOfWeek).isEqualTo(DayOfWeek.SATURDAY)
  }

  @Test
  fun `weekly next date increments by one week`() {
    val nextDate = AppointmentRepeatPeriod.WEEKLY.occurrenceDate(monday, 2)
    assertThat(nextDate.dayOfMonth).isEqualTo(20)
    assertThat(nextDate.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
  }

  @Test
  fun `fortnightly next date increments by two weeks`() {
    val nextDate = AppointmentRepeatPeriod.FORTNIGHTLY.occurrenceDate(monday, 2)
    assertThat(nextDate.dayOfMonth).isEqualTo(27)
    assertThat(nextDate.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)
  }

  @Test
  fun `monthly next date increments by one month`() {
    val nextDate = AppointmentRepeatPeriod.MONTHLY.occurrenceDate(monday, 2)
    assertThat(nextDate.dayOfMonth).isEqualTo(13)
    assertThat(nextDate.dayOfWeek).isEqualTo(DayOfWeek.THURSDAY)
  }
}
