package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import java.time.LocalDate
import java.time.LocalDateTime

object TimeSource {
  fun now(): LocalDateTime = LocalDateTime.now()

  fun today(): LocalDate = now().toLocalDate()

  fun yesterday(): LocalDate = today().minusDays(1)

  fun tomorrow(): LocalDate = today().plusDays(1)

  fun daysInPast(days: Long): LocalDate = today().minusDays(days)
}
