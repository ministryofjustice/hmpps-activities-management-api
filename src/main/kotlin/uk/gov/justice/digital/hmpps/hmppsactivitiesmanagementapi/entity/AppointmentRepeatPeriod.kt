package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.LocalDate

enum class AppointmentRepeatPeriod {
  WEEKDAY {
    override fun nextDate(date: LocalDate): LocalDate {
      val daysToAdd = if (date.dayOfWeek.value == 5) { 3L } else { 1L }
      return date.plusDays(daysToAdd)
    }
  },
  DAILY {
    override fun nextDate(date: LocalDate): LocalDate {
      return date.plusDays(1)
    }
  },
  WEEKLY {
    override fun nextDate(date: LocalDate): LocalDate {
      return date.plusWeeks(1)
    }
  },
  FORTNIGHTLY {
    override fun nextDate(date: LocalDate): LocalDate {
      return date.plusWeeks(2)
    }
  },
  MONTHLY {
    override fun nextDate(date: LocalDate): LocalDate {
      return date.plusMonths(1)
    }
  },
  ;

  abstract fun nextDate(date: LocalDate): LocalDate
}
