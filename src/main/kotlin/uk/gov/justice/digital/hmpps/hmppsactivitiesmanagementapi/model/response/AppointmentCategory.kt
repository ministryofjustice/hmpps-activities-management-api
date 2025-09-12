package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.CategoryStatus

@Schema(description = "Describes a top-level appointment category")
data class AppointmentCategory(
  @Schema(description = "The internally-generated identifier for this appointment category", example = "1")
  val id: Long,

  @Schema(description = "The appointment category code", example = "OIC")
  val code: String,

  @Schema(description = "The description of the appointment category", example = "Adjudication Hearing")
  val description: String,

  @Schema(description = "The identifier for the appointment parent category", example = "1")
  val appointmentParentCategory: AppointmentParentCategory,

  @Schema(description = "Status of the appointment category", example = "ACTIVE")
  val status: CategoryStatus,
)
