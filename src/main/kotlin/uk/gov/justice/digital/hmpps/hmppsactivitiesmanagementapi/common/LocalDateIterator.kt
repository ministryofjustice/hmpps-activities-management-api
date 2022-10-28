package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import java.time.LocalDate

class LocalDateIterator(
  startDate: LocalDate,
  private val endDate: LocalDate,
  private val stepDays: Long
) : Iterator<LocalDate> {
  private var currentDate = startDate

  override fun hasNext() = currentDate <= endDate

  override fun next(): LocalDate {
    val next = currentDate
    currentDate = currentDate.plusDays(stepDays)
    return next
  }
}
