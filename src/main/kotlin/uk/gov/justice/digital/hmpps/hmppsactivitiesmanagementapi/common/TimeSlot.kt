package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import java.time.LocalTime

enum class TimeSlot {
  AM,
  PM,
  ED,
  ;

  companion object {
    fun slot(time: LocalTime) = when (time.hour) {
      in 0..11 -> AM
      in 12..16 -> PM
      else -> ED
    }
  }
}
