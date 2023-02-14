package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "Describes the minimum education levels to be created for an activity")
data class ActivityMinimumEducationLevelCreateRequest(

  @field:NotNull(message = "Education level code  must be supplied")
  @field:Size(max = 10, message = "Education level code should not exceed {max} characters")
  @Schema(description = "The Education level code", example = "1")
  val educationLevelCode: String? = null,

  @field:NotNull(message = "Education level description  must be supplied")
  @field:Size(max = 60, message = "Education level description should not exceed {max} characters")
  @Schema(description = "The Education level description", example = "Reading Measure 1.0")
  val educationLevelDescription: String? = null,
)
