package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.CategoryStatus

@Schema(description = "Request object for creating an appointment category")
data class AppointmentCategoryRequest(

  @Schema(description = "The appointment category code", example = "OIC")
  @field:NotBlank(message = "Category code must be supplied")
  @field:Size(max = 30, message = "Category code must not exceed {max} characters")
  val code: String,

  @Schema(description = "The description of the appointment category", example = "Adjudication Hearing")
  @field:NotBlank(message = "Category description must be supplied")
  @field:Size(max = 300, message = "Category description must not exceed {max} characters")
  val description: String,

  @Schema(description = "Identifier for the appointment parent category", example = "1",)
  @field:NotNull(message = "Appointment parent category must be supplied")
  val appointmentParentCategoryId: Long,

  @Schema(description = "Status of the appointment category", example = "ACTIVE")
  @field:NotNull(message = "Appointment category status must be supplied")
  val status: CategoryStatus,
)
