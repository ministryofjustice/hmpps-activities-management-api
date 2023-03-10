package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrison as ModelRolloutPrison

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

  @Schema(description = "The date rolled out", example = "2022-09-30")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val rolloutDate: LocalDate?,

  @Schema(description = "Whether appointments are being fetched from the activities service (as opposed to the Prison API)", example = "true")
  val isAppointmentsEnabled: Boolean,

) {
  fun toModel(prison: RolloutPrison) = ModelRolloutPrison(
    id = prison.rolloutPrisonId,
    code = prison.code,
    description = prison.description,
    active = prison.active,
    rolloutDate = prison.rolloutDate,
    isAppointmentsEnabled = prison.isAppointmentsEnabled(),
  )
}
