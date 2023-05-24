package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason

data class PrisonerDeallocationRequest(

  @Schema(description = "The prisoner or prisoners to be deallocated")
  @field:NotEmpty(message = "At least one prisoner number must be supplied")
  val prisonerNumbers: List<String>?,

  @Schema(description = "The reason code for the deallocation", example = "RELEASED")
  @field:NotNull(message = "Deallocation reason code must supplied")
  val reasonCode: DeallocationReason?,
)
