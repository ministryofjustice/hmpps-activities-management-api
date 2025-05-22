package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(description = "Request object for creating an advance attendance record")
data class AdvanceAttendanceCreateRequest(

  @Schema(description = "The scheduled instance id", example = "123")
  @field:NotNull(message = "Schedule instance id must be supplied")
  val scheduleInstanceId: Long? = null,

  @Schema(description = "The prisoner number (NOMIS ID)", example = "A1234AA")
  @field:NotBlank(message = "Prisoner number must be supplied")
  @field:Size(max = 7, message = "Prisoner number must not exceed {max} characters")
  val prisonerNumber: String?,

  @Schema(description = "Should payment be issued?", example = "true")
  @field:NotNull(message = "Issue payment must be supplied")
  val issuePayment: Boolean? = null,
)
