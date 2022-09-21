package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate
import java.time.LocalDateTime

// TODO swagger docs
data class ActivityInstance(

  val id: Long,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val sessionDate: LocalDate,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val startTime: LocalDateTime,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val endTime: LocalDateTime,

  val cancelled: Boolean,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val cancelledTime: LocalDateTime? = null,

  val cancelledBy: String? = null,
)
