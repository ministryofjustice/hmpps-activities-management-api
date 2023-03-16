package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class UncancelScheduledInstanceRequest(

  @Schema(description = "The username of the user performing the unschedule operation", example = "RJ56DDE")
  @field:NotEmpty(message = "Username must be supplied")
  val username: String,

  @Schema(description = "The displayName of the user performing the unschedule operation", example = "Bob Adams")
  @field:NotEmpty(message = "Display name must be supplied")
  val displayName: String,
)
