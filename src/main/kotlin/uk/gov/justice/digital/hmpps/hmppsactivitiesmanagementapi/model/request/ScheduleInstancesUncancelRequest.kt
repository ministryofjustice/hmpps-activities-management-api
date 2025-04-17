package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Request object for uncancelling multiple schedule instances")
data class ScheduleInstancesUncancelRequest(

  @field:NotEmpty(message = "At least one scheduled instance id must be supplied")
  @Schema(description = "The scheduled instance ids to uncancel")
  val scheduleInstanceIds: List<Long>?,
)
