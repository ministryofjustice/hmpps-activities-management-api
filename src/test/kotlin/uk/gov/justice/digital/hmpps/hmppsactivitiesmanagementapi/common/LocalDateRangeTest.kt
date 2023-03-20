package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LocalDateRangeTest {

  @Test
  fun `range creation`() {
    val expected = listOf(
      "2020-01-01",
      "2020-01-02",
      "2020-01-03",
      "2020-01-04",
      "2020-01-05",
    )
    val startDate = LocalDate.of(2020, 1, 1)
    val endDate = LocalDate.of(2020, 1, 5)

    val actual = (startDate..endDate).iterator().asSequence().toList().map { it.toString() }

    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `range step`() {
    val expected = listOf(
      "2020-01-01",
      "2020-01-03",
      "2020-01-05",
    )
    val startDate = LocalDate.of(2020, 1, 1)
    val endDate = LocalDate.of(2020, 1, 5)

    val actual = (startDate..endDate step 2).iterator().asSequence().toList().map { it.toString() }

    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `range contains`() {
    val startDate = LocalDate.of(2020, 1, 1)
    val endDate = LocalDate.of(2020, 1, 5)

    val actual = LocalDate.of(2020, 1, 2) in (startDate..endDate)

    assertThat(actual).isTrue
  }

  @Test
  fun `range is empty`() {
    val expected = listOf<LocalDate>()

    val actual = LocalDateRange.EMPTY.toList()

    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `range includes dates`() {
    val startDate = LocalDate.of(2023, 1, 1)
    val endDate = LocalDate.of(2023, 1, 31)
    val range = LocalDateRange(startDate, endDate)

    startDate.datesUntil(endDate.plusDays(1)).forEach { date -> assertThat(range.includes(date)).isTrue }
  }

  @Test
  fun `range excludes dates`() {
    val startDate = LocalDate.of(2023, 1, 1)
    val endDate = LocalDate.of(2023, 1, 31)
    val range = LocalDateRange(startDate, endDate)

    assertThat(range.includes(startDate.minusDays(1))).isFalse
    assertThat(range.includes(endDate.plusDays(1))).isFalse
  }
}
