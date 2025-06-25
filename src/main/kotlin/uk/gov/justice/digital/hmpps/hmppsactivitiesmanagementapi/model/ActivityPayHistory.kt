package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Describes the pay rates history which apply to an activity")
data class ActivityPayHistory(

  @Schema(description = "The internally-generated ID for this activity pay history", example = "123456")
  val id: Long,

  @Schema(description = "The NOMIS code for the incentive/earned privilege level", example = "BAS")
  val incentiveNomisCode: String? = null,

  @Schema(description = "The incentive/earned privilege level", example = "Basic")
  val incentiveLevel: String? = null,

  @Schema(description = "The pay band id for this activity pay history", example = "123456")
  val prisonPayBand: uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand,

  @Schema(description = "The earning rate for one half day session for someone of this incentive level and pay band (in pence)", example = "150")
  val rate: Int? = null,

  @Schema(description = "The effective start date for this pay rate", example = "2024-06-18")
  val startDate: LocalDate? = null,

  @Schema(description = "The actual change to this pay rate", example = "New pay rate added: £1.00")
  val changedDetails: String? = null,

  @Schema(description = "The date and time when this pay rate was changed", example = "2022-09-01'T'09:01:02.964")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
  val changedTime: LocalDateTime? = null,

  @Schema(description = "The person who changed this pay rate", example = "ABC123 - A. Smith")
  val changedBy: String? = null,
)
