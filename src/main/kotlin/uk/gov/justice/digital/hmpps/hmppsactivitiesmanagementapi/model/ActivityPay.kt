package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Describes the pay rates and bands which apply to an activity")
data class ActivityPay(

  @Schema(description = "The internally-generated ID for this activity pay", example = "123456")
  val id: Long,

  @Schema(description = "The NOMIS code for the incentive/earned privilege level", example = "BAS")
  val incentiveNomisCode: String,

  @Schema(description = "The incentive/earned privilege level", example = "Basic")
  val incentiveLevel: String,

  @Schema(description = "The pay band for this activity pay")
  val prisonPayBand: PrisonPayBand,

  @Schema(description = "The earning rate for one half day session for someone of this incentive level and pay band (in pence)", example = "150")
  val rate: Int? = null,

  @Schema(description = "Where payment is related to produced amounts of a product, this indicates the payment rate (in pence) per pieceRateItems produced", example = "150")
  val pieceRate: Int? = null,

  @Schema(description = "Where payment is related to the number of items produced in a batch of a product, this is the batch size that attract 1 x pieceRate", example = "10")
  val pieceRateItems: Int? = null,

  @Schema(description = "The effective start date for this pay rate", example = "2024-06-18")
  val startDate: LocalDate? = null,
)
