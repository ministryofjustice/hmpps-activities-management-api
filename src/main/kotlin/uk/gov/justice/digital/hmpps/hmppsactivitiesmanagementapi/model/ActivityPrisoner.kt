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

  val active: Boolean = false,

  val allocationAt: LocalDateTime? = null,

  val allocatedBy: String? = null,

  val deallocatedAt: LocalDateTime? = null,

  val deallocatedBy: String? = null,

  val deallocationReason: String? = null,
)
