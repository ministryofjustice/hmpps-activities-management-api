package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

@Schema(
  description =
  """
  Describes the deallocation of one or more prisoners from an activity schedule on a given date in future.
  All of the allocations associated with the schedule must be active or suspended but not ended.
  """,
)
data class PrisonerDeallocationRequest(

  @Schema(
    description =
    "The prisoner or prisoners to be deallocated. Must be allocated to the schedule affected by the request.",
  )
  @field:NotEmpty(message = "One or more prisoner numbers for the deallocation request must be supplied.")
  val prisonerNumbers: List<String>?,

  @Schema(
    description = "The reason code for the deallocation",
    example = "RELEASED",
    allowableValues = ["OTHER", "PERSONAL", "PROBLEM", "REMOVED", "SECURITY", "UNACCEPTABLE_ATTENDANCE", "UNACCEPTABLE_BEHAVIOUR", "WITHDRAWN"],
  )
  @field:NotEmpty(message = "The reason code for the deallocation request must supplied.")
  val reasonCode: String?,

  @Schema(
    description =
    "The future date on which this allocation will end. Must not exceed the end date of the allocation, schedule or activity.",
    example = "2023-05-24",
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @Future
  @NotNull(message = "The end date for the deallocation request must supplied.")
  val endDate: LocalDate?,
)
