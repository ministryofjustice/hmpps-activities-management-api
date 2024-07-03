package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment

import java.time.DayOfWeek
import java.time.LocalDate

enum class AppointmentFrequency {
  WEEKDAY {
    override fun appointmentDate(startDate: LocalDate, sequenceNumber: Int): LocalDate {
      var date = startDate
      if (startDate.dayOfWeek == DayOfWeek.SATURDAY || startDate.dayOfWeek == DayOfWeek.SUNDAY) {
        // User has specified that an appointment repeating on weekdays (Monday to Friday) starts on a weekend.
        // That is not supported by the default calculation below so either return the start date for the first appointment...
        if (sequenceNumber == 1) {
          return startDate
        }
        // Or use the previous Friday's date for the standard calculation
        if (startDate.dayOfWeek == DayOfWeek.SATURDAY) {
          date = startDate.minusDays(1)
        }
        if (startDate.dayOfWeek == DayOfWeek.SUNDAY) {
          date = startDate.minusDays(2)
        }
      }

      val dayOfWeekValue = date.dayOfWeek.value
      val weeks: Long = (sequenceNumber - 1L) / 5
      val remainder: Long = (sequenceNumber - 1L) % 5
      val daysToAdd = weeks * 7 + if (remainder + dayOfWeekValue > 5) remainder + 2 else remainder
      return date.plusDays(daysToAdd)
    }
  },
  DAILY {
    override fun appointmentDate(startDate: LocalDate, sequenceNumber: Int): LocalDate {
      return startDate.plusDays(sequenceNumber - 1L)
    }
  },
  WEEKLY {
    override fun appointmentDate(startDate: LocalDate, sequenceNumber: Int): LocalDate {
      return startDate.plusWeeks(sequenceNumber - 1L)
    }
  },
  FORTNIGHTLY {
    override fun appointmentDate(startDate: LocalDate, sequenceNumber: Int): LocalDate {
      return startDate.plusWeeks(sequenceNumber * 2L - 2L)
    }
  },
  MONTHLY {
    override fun appointmentDate(startDate: LocalDate, sequenceNumber: Int): LocalDate {
      return startDate.plusMonths(sequenceNumber - 1L)
    }
  },
  ;

  /**
   * @param startDate The date of the first appointment
   * @param sequenceNumber Sequence number of the appointment date that should be generated. Starts at 1 not 0
   */
  abstract fun appointmentDate(startDate: LocalDate, sequenceNumber: Int): LocalDate
}
