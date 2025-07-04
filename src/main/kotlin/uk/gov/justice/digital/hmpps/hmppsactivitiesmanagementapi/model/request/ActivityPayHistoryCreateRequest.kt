package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "Describes the pay rates history to be created for an activity")
data class ActivityPayHistoryCreateRequest(

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

  @Schema(
    description = "The effective start date for this pay rate",
    example = "2022-12-23",
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val startDate: LocalDate? = null,

  @field:NotEmpty(message = "Changed details must be supplied")
  @field:Size(max = 500, message = "Changed details should not exceed {max} characters")
  @Schema(description = "The pay rate change details", example = "New pay rate added: £1.00")
  val changedDetails: String? = null,

  @field:NotEmpty(message = "Changed by must be supplied")
  @field:Size(max = 100, message = "Changed by should not exceed {max} characters")
  @Schema(description = "The person who changed this pay rate", example = "joebloggs")
  val changedBy: String? = null,
)
