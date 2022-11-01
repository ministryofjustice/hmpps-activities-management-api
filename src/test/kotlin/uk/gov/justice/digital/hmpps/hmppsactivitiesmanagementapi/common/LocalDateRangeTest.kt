package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LocalDateRangeTest {

  @Test
  fun testRange() {
    val expected = listOf(
      "2020-01-01",
      "2020-01-02",
      "2020-01-03",
      "2020-01-04",
      "2020-01-05"
    )
    val startDate = LocalDate.of(2020, 1, 1)
    val endDate = LocalDate.of(2020, 1, 5)

    val actual = (startDate..endDate).iterator().asSequence().toList().map { it.toString() }

    assertEquals(expected, actual)
  }

  @Test
  fun testStep() {
    val expected = listOf(
      "2020-01-01",
      "2020-01-03",
      "2020-01-05"
    )
    val startDate = LocalDate.of(2020, 1, 1)
    val endDate = LocalDate.of(2020, 1, 5)

    val actual = (startDate..endDate step 2).iterator().asSequence().toList().map { it.toString() }

    assertEquals(expected, actual)
  }

  @Test
  fun testContains() {
    val startDate = LocalDate.of(2020, 1, 1)
    val endDate = LocalDate.of(2020, 1, 5)

    val actual = LocalDate.of(2020, 1, 2) in (startDate..endDate)

    assertEquals(true, actual)
  }

  @Test
  fun testEmpty() {
    val expected = listOf<LocalDate>()

    val actual = LocalDateRange.EMPTY.toList()

    assertEquals(expected, actual)
  }
}
