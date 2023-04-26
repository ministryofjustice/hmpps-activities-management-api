package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

@Schema(description = "Describes the pay rates and bands to be created for an activity")
data class ActivityPayCreateRequest(

  @field:NotEmpty(message = "NOMIS code for the minimum incentive level must be supplied")
  @field:Size(max = 3, message = "NOMIS code for the incentive level should not exceed {max} characters")
  @Schema(description = "The NOMIS code for the incentive/earned privilege level", example = "BAS")
  val incentiveNomisCode: String?,

  @field:NotEmpty(message = "Minimum incentive level must be supplied")
  @field:Size(max = 50, message = "Incentive level should not exceed {max} characters")
  @Schema(description = "The incentive/earned privilege level", example = "Basic")
  val incentiveLevel: String?,

  @field:NotNull(message = "Pay band must be supplied")
  @Schema(description = "The id of the prison pay band used", example = "1")
  val payBandId: Long? = null,

  @field:Positive(message = "The earning rate must be a positive integer")
  @Schema(description = "The earning rate for one half day session for someone of this incentive level and pay band (in pence)", example = "150")
  val rate: Int? = null,

  @field:Positive(message = "The piece rate must be a positive integer")
  @Schema(description = "Where payment is related to produced amounts of a product, this indicates the payment rate (in pence) per pieceRateItems produced", example = "150")
  val pieceRate: Int? = null,

  @field:Positive(message = "The piece rate items must be a positive integer")
  @Schema(description = "Where payment is related to the number of items produced in a batch of a product, this is the batch size that attract 1 x pieceRate", example = "10")
  val pieceRateItems: Int? = null,
)
