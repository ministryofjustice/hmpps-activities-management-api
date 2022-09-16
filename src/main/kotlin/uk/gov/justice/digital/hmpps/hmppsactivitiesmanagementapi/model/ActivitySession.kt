package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import java.time.LocalDate
import java.time.LocalDateTime

// TODO swagger docs
data class ActivitySession(

  val id: Long,

  val instances: List<ActivityInstance> = emptyList(),

  val prisoners: List<ActivityPrisoner> = emptyList(),

  val description: String,

  val suspendUntil: LocalDate? = null,

  val startTime: LocalDateTime,

  val endTime: LocalDateTime,

  val internalLocationId: Int? = null,

  val internalLocationCode: String? = null,

  val internalLocationDescription: String? = null,

  val capacity: Int,

  val daysOfWeek: String
)
