package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class ActivityPrisoner(

  @Schema(description = "The internal ID for this activity prisoner", example = "123456")
  val id: Long,

  @Schema(description = "The prison identifier for this offender", example = "A1234AA")
  val prisonerNumber: String,

// TODO swagger docs
  @Schema(description = "The incentive/earned privilege (level) for this offender", example = "?????")
  val iepLevel: String? = null,

// TODO swagger docs
  val payBand: String? = null,

  @Schema(description = "The date and time when the prisoner can start the activity", example = "10/09/2022 9:00")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val startDate: LocalDate? = null,

  @Schema(description = "The date and time when the prisoner can no longer attend the activity", example = "10/09/2023 9:00")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val endDate: LocalDate? = null,

  @Schema(description = "Flag to indicate if this activity prisoner is presently active", example = "true")
  val active: Boolean = true,

  @Schema(description = "The date and time the prisoner was allocated to the activity in question", example = "01/09/2022 9:00")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val allocatedTime: LocalDateTime? = null,

  @Schema(description = "The person whom allocated the prisoner to the activity in question", example = "Mr Blogs")
  val allocatedBy: String? = null,

  @Schema(description = "The date and time the prisoner was deallocated from the activity in question", example = "02/09/2022 9:00")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val deallocatedTime: LocalDateTime? = null,

  @Schema(description = "The person whom deallocated the prisoner from the activity in question", example = "Mrs Blogs")
  val deallocatedBy: String? = null,

  @Schema(description = "The descriptive reason the prisoner was deallocated from the activity in question", example = "Mrs Blogs")
  val deallocatedReason: String? = null,
)
