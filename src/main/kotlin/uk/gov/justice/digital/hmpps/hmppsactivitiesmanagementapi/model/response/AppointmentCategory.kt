package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentParentCategory

@Schema(description = "Describes a top-level appointment category")
data class AppointmentCategory(
  @Schema(
    description = "The internally-generated identifier for this appointment category",
    example = "1",
  )
  val id: Long,

  @Schema(description = "The appointment category code", example = "LEISURE_SOCIAL")
  val code: String,

  @Schema(description = "The description of the appointment category", example = "Such as association, library time and social clubs, like music or art")
  val description: String?,

  @Schema(
    description = "The internally generated identifier for the appointment parent category",
    example = "1",
  )
  val appointmentParentCategory: AppointmentParentCategory?,

  @Schema(description = "Status of the appointment category", example = "Leisure and social")
  val status: String?,
)
