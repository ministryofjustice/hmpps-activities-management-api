package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "A basic activity, schedule and category for use where limited IDs only are required")
data class ActivityBasic(
  @Schema(description = "The prison code where this activity takes place", example = "PVI")
  val prisonCode: String,

  @Schema(description = "The internally-generated ID for this activity", example = "123456")
  val activityId: Long = 0,

  @Schema(description = "The internally-generated ID for this activity schedule (assumes 1-2-1 with activity)", example = "7654321")
  val activityScheduleId: Long = 0,

  @Schema(description = "A brief summary description of this activity for use in forms and lists", example = "Maths level 1")
  val summary: String?,

  @Schema(description = "The start date for this activity", example = "2023-10-11")
  val startDate: LocalDate,

  @Schema(description = "The end date for this activity (can be null)", example = "2024-12-01")
  val endDate: LocalDate?,

  @Schema(description = "The internally generated category ID associated with this activity", example = "1")
  val categoryId: Long = 0,

  @Schema(description = "The category code that matches NOMIS program service code for this activity category", example = "SAA-EDUCATION")
  val categoryCode: String,

  @Schema(description = "The category name", example = "Education")
  val categoryName: String,
)
