package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a prisoner scheduled to attend to an activity")
data class ScheduledAttendee(
  @Schema(description = "The internal ID of the scheduled instance", example = "1")
  val scheduledInstanceId: Long,

  @Schema(description = "The internal ID of the allocation", example = "1")
  val allocationId: Long,

  @Schema(description = "The candidate's prisoner number", example = "GF10101")
  val prisonerNumber: String,

  @Schema(description = "The booking id of the prisoner", example = "10001")
  val bookingId: Long?,

  @Schema(description = "Set to true if this prisoner is suspended from the scheduled event", example = "false")
  val suspended: Boolean = false,
)
