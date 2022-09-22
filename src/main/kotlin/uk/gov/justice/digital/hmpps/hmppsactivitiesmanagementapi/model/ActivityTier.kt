package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

// TODO swagger docs
data class ActivityTier(

  @Schema(description = "The internal ID for this activity tier", example = "123456")
  val id: Long,

  val code: String,

  val description: String
)
