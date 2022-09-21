package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate
import java.time.LocalDateTime

// TODO swagger docs
data class ActivitySession(

  val id: Long,

  val instances: List<ActivityInstance> = emptyList(),

  val prisoners: List<ActivityPrisoner> = emptyList(),

  val description: String,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val suspendUntil: LocalDate? = null,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val startTime: LocalDateTime,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val endTime: LocalDateTime,

  val internalLocationId: Int? = null,

  val internalLocationCode: String? = null,

  val internalLocationDescription: String? = null,

  val capacity: Int,

  val daysOfWeek: String
)
