package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the minimum education levels which apply to an activity")
data class ActivityMinimumEducationLevel(

  @Schema(description = "The internally-generated ID for this activity minimum education level", example = "123456")
  val id: Long,

  @Schema(description = "The education level code", example = "Basic")
  val educationLevelCode: String,

  @Schema(description = "The education level description", example = "Basic")
  val educationLevelDescription: String,

)
