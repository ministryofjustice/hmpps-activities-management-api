package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

data class ActivityWaiting(

  val id: Long,

  val prisonerNumber: String,

  val priority: Int,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val createdTime: LocalDateTime,

  val createdBy: String,
)
