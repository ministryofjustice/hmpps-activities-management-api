package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import java.time.LocalDate
import java.time.LocalDateTime

// TODO swagger docs
data class ActivityPrisoner(

  val id: Long,

  val prisonerNumber: String,

  val iepLevel: String? = null,

  val payBand: String? = null,

  val startDate: LocalDate? = null,

  val endDate: LocalDate? = null,

  val active: Boolean = true,

  val allocatedTime: LocalDateTime? = null,

  val allocatedBy: String? = null,

  val deallocatedTime: LocalDateTime? = null,

  val deallocatedBy: String? = null,

  val deallocatedReason: String? = null,
)
