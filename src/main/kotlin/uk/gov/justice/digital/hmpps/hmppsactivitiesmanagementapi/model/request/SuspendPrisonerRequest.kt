package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.annotation.Nullable
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import java.time.LocalDate

data class SuspendPrisonerRequest(

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
  @Schema(description = "The date when the prisoner will be suspended from the activity", example = "2023-09-10")
  @JsonFormat(pattern = "yyyy-MM-dd")
  @field:FutureOrPresent(message = "Suspension start date must not be in the past")
  val suspendFrom: LocalDate? = null,

  @Schema(description = "Describes a case note to be added to the prisoner's profile as part of the suspension.")
  @field:Valid
  @field:Nullable
  val suspensionCaseNote: AddCaseNoteRequest? = null,

  // TODO: make status mandatory after integration with the UI
  @Schema(
    description = "The type of suspension. Only SUSPENDED or SUSPENDED_WITH_PAY are allowed when suspending",
    example = "SUSPENDED_WITH_PAY",
  )
  @field:Valid
  @field:Nullable
  val status: PrisonerStatus? = null,
) {
  @AssertTrue(message = "Only 'SUSPENDED' or 'SUSPENDED_WITH_PAY' are allowed for status")
  private fun isStatus() =
    status == null || listOf(PrisonerStatus.SUSPENDED, PrisonerStatus.SUSPENDED_WITH_PAY).contains(status)
}
