package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import java.time.LocalDate
import java.time.LocalDateTime

// TODO swagger docs
data class ActivityInstance(

  val id: Long,

  val sessionDate: LocalDate,

  val startTime: LocalDateTime,

  val endTime: LocalDateTime,

  val internalLocationId: Int? = null,

  val cancelled: Boolean,

  val cancelledAt: LocalDateTime? = null,

  val cancelledBy: String? = null,
)
