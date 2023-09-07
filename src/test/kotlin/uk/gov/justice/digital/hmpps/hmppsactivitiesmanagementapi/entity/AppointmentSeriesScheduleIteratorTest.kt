package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AppointmentSeriesScheduleIteratorTest {
  @Test
  fun `weekday repeat period for two weeks over month end`() {
    val wednesdayFeb22nd2023 = LocalDate.of(2023, 2, 22)
    val iterator = AppointmentSeriesScheduleIterator(wednesdayFeb22nd2023, AppointmentFrequency.WEEKDAY, 10)
    val dates = iterator.asSequence().map { it.format(DateTimeFormatter.ofPattern("E d MMMM")) }.toList()

    assertThat(dates).containsExactly(
      "Wed 22 February",
      "Thu 23 February",
      "Fri 24 February",
      "Mon 27 February",
      "Tue 28 February",
      "Wed 1 March",
      "Thu 2 March",
      "Fri 3 March",
      "Mon 6 March",
      "Tue 7 March",
    )
  }

  @Test
  fun `weekday repeat period for two weeks starting on saturday`() {
    val saturdayFeb25th2023 = LocalDate.of(2023, 2, 25)
    val iterator = AppointmentSeriesScheduleIterator(saturdayFeb25th2023, AppointmentFrequency.WEEKDAY, 10)
    val dates = iterator.asSequence().map { it.format(DateTimeFormatter.ofPattern("E d MMMM")) }.toList()

    assertThat(dates).containsExactly(
      "Sat 25 February",
      "Mon 27 February",
      "Tue 28 February",
      "Wed 1 March",
      "Thu 2 March",
      "Fri 3 March",
      "Mon 6 March",
      "Tue 7 March",
      "Wed 8 March",
      "Thu 9 March",
    )
  }

  @Test
  fun `weekday repeat period for two weeks starting on sunday`() {
    val sundayFeb26th2023 = LocalDate.of(2023, 2, 26)
    val iterator = AppointmentSeriesScheduleIterator(sundayFeb26th2023, AppointmentFrequency.WEEKDAY, 10)
    val dates = iterator.asSequence().map { it.format(DateTimeFormatter.ofPattern("E d MMMM")) }.toList()

    assertThat(dates).containsExactly(
      "Sun 26 February",
      "Mon 27 February",
      "Tue 28 February",
      "Wed 1 March",
      "Thu 2 March",
      "Fri 3 March",
      "Mon 6 March",
      "Tue 7 March",
      "Wed 8 March",
      "Thu 9 March",
    )
  }

  @Test
  fun `daily repeat period for two weeks over month end`() {
    val wednesdayFeb22nd2023 = LocalDate.of(2023, 2, 22)
    val iterator = AppointmentSeriesScheduleIterator(wednesdayFeb22nd2023, AppointmentFrequency.DAILY, 14)
    val dates = iterator.asSequence().map { it.format(DateTimeFormatter.ofPattern("E d MMMM")) }.toList()

    assertThat(dates).containsExactly(
      "Wed 22 February",
      "Thu 23 February",
      "Fri 24 February",
      "Sat 25 February",
      "Sun 26 February",
      "Mon 27 February",
      "Tue 28 February",
      "Wed 1 March",
      "Thu 2 March",
      "Fri 3 March",
      "Sat 4 March",
      "Sun 5 March",
      "Mon 6 March",
      "Tue 7 March",
    )
  }

  @Test
  fun `weekly repeat period for four weeks over month end`() {
    val wednesdayFeb15th2023 = LocalDate.of(2023, 2, 15)
    val iterator = AppointmentSeriesScheduleIterator(wednesdayFeb15th2023, AppointmentFrequency.WEEKLY, 4)
    val dates = iterator.asSequence().map { it.format(DateTimeFormatter.ofPattern("E d MMMM")) }.toList()

    assertThat(dates).containsExactly(
      "Wed 15 February",
      "Wed 22 February",
      "Wed 1 March",
      "Wed 8 March",
    )
  }

  @Test
  fun `Fortnightly repeat period for eight weeks over month end`() {
    val wednesdayFeb15th2023 = LocalDate.of(2023, 2, 15)
    val iterator = AppointmentSeriesScheduleIterator(wednesdayFeb15th2023, AppointmentFrequency.FORTNIGHTLY, 4)
    val dates = iterator.asSequence().map { it.format(DateTimeFormatter.ofPattern("E d MMMM")) }.toList()

    assertThat(dates).containsExactly(
      "Wed 15 February",
      "Wed 1 March",
      "Wed 15 March",
      "Wed 29 March",
    )
  }

  @Test
  fun `monthly uses same date for all months`() {
    val mondayJan31st2022 = LocalDate.of(2022, 1, 15)
    val iterator = AppointmentSeriesScheduleIterator(mondayJan31st2022, AppointmentFrequency.MONTHLY, 12)
    val dates = iterator.asSequence().map { it.format(DateTimeFormatter.ofPattern("E d MMMM")) }.toList()

    assertThat(dates).containsExactly(
      "Sat 15 January",
      "Tue 15 February",
      "Tue 15 March",
      "Fri 15 April",
      "Sun 15 May",
      "Wed 15 June",
      "Fri 15 July",
      "Mon 15 August",
      "Thu 15 September",
      "Sat 15 October",
      "Tue 15 November",
      "Thu 15 December",
    )
  }

  @Test
  fun `monthly last day of month uses nearest last day for all months with less than 31 days`() {
    val mondayJan31st2022 = LocalDate.of(2022, 1, 31)
    val iterator = AppointmentSeriesScheduleIterator(mondayJan31st2022, AppointmentFrequency.MONTHLY, 12)
    val dates = iterator.asSequence().map { it.format(DateTimeFormatter.ofPattern("E d MMMM")) }.toList()

    assertThat(dates).containsExactly(
      "Mon 31 January",
      "Mon 28 February",
      "Thu 31 March",
      "Sat 30 April",
      "Tue 31 May",
      "Thu 30 June",
      "Sun 31 July",
      "Wed 31 August",
      "Fri 30 September",
      "Mon 31 October",
      "Wed 30 November",
      "Sat 31 December",
    )
  }

  @Test
  fun `monthly last day of month uses nearest last day for all months with less than 31 days - leap year`() {
    val wednesdayJan31st2024 = LocalDate.of(2024, 1, 31)
    val iterator = AppointmentSeriesScheduleIterator(wednesdayJan31st2024, AppointmentFrequency.MONTHLY, 12)
    val dates = iterator.asSequence().map { it.format(DateTimeFormatter.ofPattern("E d MMMM")) }.toList()

    assertThat(dates).containsExactly(
      "Wed 31 January",
      "Thu 29 February",
      "Sun 31 March",
      "Tue 30 April",
      "Fri 31 May",
      "Sun 30 June",
      "Wed 31 July",
      "Sat 31 August",
      "Mon 30 September",
      "Thu 31 October",
      "Sat 30 November",
      "Tue 31 December",
    )
  }

  @Test
  fun `monthly second to last day of month uses nearest last day for all months with less than 31 days`() {
    val sundayJan30st2022 = LocalDate.of(2022, 1, 30)
    val iterator = AppointmentSeriesScheduleIterator(sundayJan30st2022, AppointmentFrequency.MONTHLY, 12)
    val dates = iterator.asSequence().map { it.format(DateTimeFormatter.ofPattern("E d MMMM")) }.toList()

    assertThat(dates).containsExactly(
      "Sun 30 January",
      "Mon 28 February",
      "Wed 30 March",
      "Sat 30 April",
      "Mon 30 May",
      "Thu 30 June",
      "Sat 30 July",
      "Tue 30 August",
      "Fri 30 September",
      "Sun 30 October",
      "Wed 30 November",
      "Fri 30 December",
    )
  }
}
