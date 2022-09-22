package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

data class ActivityCategory(

  @Schema(description = "The internal ID for this activity category", example = "123456")
  val id: Long,

// TODO finish swagger docs example for category code ????
  @Schema(description = "The code for this activity category", example = "????")
  val code: String,

  @Schema(description = "The description for this activity category", example = "Education")
  val description: String
)
