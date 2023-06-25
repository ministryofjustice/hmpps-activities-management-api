package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.projections

import java.time.LocalDate

data class ActivityBasic(
  val prisonCode: String,
  val activityId: Long = 0,
  val activityScheduleId: Long = 0,
  var summary: String?,
  var startDate: LocalDate,
  var endDate: LocalDate?,
)
