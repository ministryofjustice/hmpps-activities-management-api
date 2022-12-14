package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Describes one instance of a prison which may or may not be active (rolled out)")
data class RolloutPrison(

  @Schema(description = "The internally-generated ID for this prison", example = "123456")
  val id: Long,

  @Schema(description = "The code for this prison", example = "PVI")
  val code: String,

  @Schema(description = "The description for this prison", example = "HMP Pentonville")
  val description: String,

  @Schema(description = "Flag to indicate if this prison is presently active", example = "true")
  var active: Boolean,

  @Schema(description = "The date rolled out", example = "30/09/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val rolloutDate: LocalDate?,
)
