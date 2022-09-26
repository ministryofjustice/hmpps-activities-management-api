package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a category of activity")
data class ActivityCategory(

  @Schema(description = "The internally generated ID for this activity category", example = "123456")
  val id: Long,

  @Schema(description = "The category code - one of a defined set.", example = "Education, Prison Industry, Maintenance, Intervention")
  val code: String,

  @Schema(description = "The description for this activity category", example = "Education classes")
  val description: String
)
