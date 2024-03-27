package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class UnsuspendPrisonerRequest(

  @Schema(description = "The prisoner number (NOMIS ID)", example = "A1234AA")
  @field:NotBlank(message = "Prisoner number must be supplied")
  @field:Size(max = 7, message = "Prisoner number must not exceed {max} characters")
  val prisonerNumber: String?,

  @field:NotEmpty(message = "At least one allocation identifier must be supplied")
  @Schema(
    description = "The allocation or allocations affected by the suspensions request. They must all be for the same prisoner",
    example = "[1,2,3,4]",
  )
  val allocationIds: List<Long> = emptyList(),

  @field:NotNull
  @Schema(description = "The date when the prisoner will be suspended till from the activity", example = "2023-09-10")
  @JsonFormat(pattern = "yyyy-MM-dd")
  @field:FutureOrPresent(message = "Suspension end date must not be in the past")
  val suspendUntil: LocalDate? = null,
)
