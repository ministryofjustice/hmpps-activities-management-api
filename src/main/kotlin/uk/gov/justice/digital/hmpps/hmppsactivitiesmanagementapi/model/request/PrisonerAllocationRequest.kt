package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class PrisonerAllocationRequest(

  @Schema(description = "The prisoner number (Nomis ID)", example = "A1234AA")
  @field:NotBlank(message = "Prisoner number must be supplied")
  @field:Size(max = 7, message = "Prisoner number cannot be more than 7 characters")
  val prisonerNumber: String?,

  @Schema(
    description = "Where a prison uses pay bands to differentiate earnings, this is the pay band code given to this prisoner",
    example = "1",
  )
  @field:NotNull(message = "Pay band must be supplied")
  val payBandId: Long? = null,
)
