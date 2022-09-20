package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import java.time.LocalDateTime

data class ActivityWaiting(

  val id: Long,

  val prisonerNumber: String,

  val priority: Int,

  val createdTime: LocalDateTime,

  val createdBy: String,
)
