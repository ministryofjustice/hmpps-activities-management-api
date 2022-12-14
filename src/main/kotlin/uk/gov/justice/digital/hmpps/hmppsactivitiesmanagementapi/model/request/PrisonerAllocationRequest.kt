package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

data class PrisonerAllocationRequest(
  @Schema(description = "The internally-generated ID for this activity schedule", example = "123456")
  @field:NotNull
  val scheduleId: Long,

  @Schema(description = "The prisoner number (Nomis ID)", example = "A1234AA")
  @field:NotBlank
  val prisonerNumber: String,

  @Schema(
    description = "The incentive/earned privilege (level) for this offender allocation",
    example = "BAS, STD, ENH"
  )
  @field:NotBlank
  val incentiveLevel: String,

  @Schema(
    description = "Where a prison uses pay bands to differentiate earnings, this is the pay band code given to this prisoner",
    example = "A"
  )
  @field:NotBlank
  val payBand: String
)
