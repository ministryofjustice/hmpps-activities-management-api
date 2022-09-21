package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate
import java.time.LocalDateTime

// TODO swagger docs
data class ActivityPrisoner(

  val id: Long,

  val prisonerNumber: String,

  val iepLevel: String? = null,

  val payBand: String? = null,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val startDate: LocalDate? = null,

  @JsonFormat(pattern = "dd/MM/yyyy")
  val endDate: LocalDate? = null,

  val active: Boolean = true,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val allocatedTime: LocalDateTime? = null,

  val allocatedBy: String? = null,

  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val deallocatedTime: LocalDateTime? = null,

  val deallocatedBy: String? = null,

  val deallocatedReason: String? = null,
)
