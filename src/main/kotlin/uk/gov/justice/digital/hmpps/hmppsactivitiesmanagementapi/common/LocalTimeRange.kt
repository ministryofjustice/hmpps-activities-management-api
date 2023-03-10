package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import java.time.LocalTime

data class LocalTimeRange(
  val start: LocalTime,
  val end: LocalTime,
)
