package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "Request object for creating an advance attendance record")
data class AppointmentCategoryRequest(

  @Schema(description = "The appointment category code", example = "LEISURE_SOCIAL")
  @field:NotBlank(message = "Category code must be supplied")
  @field:Size(max = 30, message = "Category code must not exceed {max} characters")
  val code: String,

  @Schema(description = "The description of the appointment category", example = "Such as association, library time and social clubs, like music or art")
  @field:Size(max = 300, message = "Category description must not exceed {max} characters")
  val description: String? = null,

  @Schema(
    description = "Identifier for the appointment parent category",
    example = "1",
  )
  val appointmentParentCategoryId: Long? = null,

  @Schema(description = "Status of the appointment category", example = "ACTIVE")
  @field:Size(max = 30, message = "Status must not exceed {max} characters")
  val status: String? = "ACTIVE",
)
