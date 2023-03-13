package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.LocalDate

class AppointmentScheduleIterator(
  val startDate: LocalDate,
  private val repeatPeriod: AppointmentRepeatPeriod,
  private val repeatCount: Int,
) : Iterator<LocalDate> {
  private var date = startDate
  private var count = 0

  override fun hasNext() = count < repeatCount

  override fun next(): LocalDate {
    val next = date
    date = repeatPeriod.nextDate(date)
    count++
    return next
  }
}
