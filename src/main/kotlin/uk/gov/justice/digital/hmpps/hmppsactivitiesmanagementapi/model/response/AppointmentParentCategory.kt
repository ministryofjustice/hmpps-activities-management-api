package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a top-level appointment parent category")
data class AppointmentParentCategory(
  @Schema(
    description = "The internally-generated identifier for this appointment parent category",
    example = "1",
  )
  val id: Long,

  @Schema(description = "The appointment parent category name", example = "LEISURE_SOCIAL")
  val name: String,

  @Schema(description = "The description of the appointment parent category", example = "Such as association, library time and social clubs, like music or art")
  val description: String? = null,
)
