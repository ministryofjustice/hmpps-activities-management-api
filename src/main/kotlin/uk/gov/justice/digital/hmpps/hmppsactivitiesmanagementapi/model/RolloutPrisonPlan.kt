package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Describes the rollout plan of a prison which may or may not be rolled out")
data class RolloutPrisonPlan(

  @Schema(description = "The prison code of the requested prison", example = "PVI")
  var prisonCode: String,

  @Schema(description = "Flag to indicate if this prison is presently rolled out for activities", example = "true")
  var activitiesRolledOut: Boolean,

  @Schema(description = "The date activities rolled out", example = "2022-09-30")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val activitiesRolloutDate: LocalDate?,

  @Schema(description = "Flag to indicate if this prison is presently rolled out for appointments", example = "true")
  var appointmentsRolledOut: Boolean,

  @Schema(description = "The date activities rolled out", example = "2022-09-30")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val appointmentsRolloutDate: LocalDate?,
)
