package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AppointmentScheduleTest {
  @Test
  fun `monthly last day of month uses nearest last day for all months with less than 31 days`() {
    val jan31st2022 = LocalDate.of(2022, 1, 31)
    val iterator = AppointmentScheduleIterator(jan31st2022, AppointmentRepeatPeriod.MONTHLY, 12)
    val dates = iterator.asSequence().map { it.format(DateTimeFormatter.ofPattern("dd MMMM")) }.toList()

    assertThat(dates).containsExactly(
      "31 January",
      "28 February",
      "31 March",
      "30 April",
      "31 May",
      "30 June",
      "31 July",
      "31 August",
      "30 September",
      "31 October",
      "31 November",
      "31 December"
    )
  }

  fun `monthly last day of month uses nearest last day for all months with less than 31 days - leap year`() {
    val jan31st2024 = LocalDate.of(2024, 1, 31)
    val iterator = AppointmentScheduleIterator(jan31st2024, AppointmentRepeatPeriod.MONTHLY, 12)
    val dates = iterator.asSequence().map { it.format(DateTimeFormatter.ofPattern("dd MMMM")) }.toList()

    assertThat(dates).containsExactly(
      "31 January",
      "29 February",
      "31 March",
      "30 April",
      "31 May",
      "30 June",
      "31 July",
      "31 August",
      "30 September",
      "31 October",
      "31 November",
      "31 December"
    )
  }
}
