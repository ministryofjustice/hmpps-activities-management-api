package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import java.time.LocalDate

data class LocalDateRange(
  override val start: LocalDate,
  override val endInclusive: LocalDate,
  private val stepDays: Long = 1
) : Iterable<LocalDate>, ClosedRange<LocalDate> {

  override fun iterator(): Iterator<LocalDate> =
    LocalDateIterator(start, endInclusive, stepDays)

  infix fun step(days: Long) = LocalDateRange(start, endInclusive, days)

  companion object {
    val EMPTY: LocalDateRange = LocalDateRange(LocalDate.ofEpochDay(1), LocalDate.ofEpochDay(0))
  }
}
