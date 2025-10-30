package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus.APPROVED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus.DECLINED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus.PENDING
import java.time.LocalDate

@Schema(
  description =
  """
  Describes a single waiting list application for a prisoner to an activity.
  """,
)
data class PrisonerWaitingListApplicationRequest(

  @Schema(
    description = "The internally-generated ID for this activity schedule (assumes 1-2-1 with activity)",
    example = "7654321",
  )
  @field:NotNull(message = "Activity schedule identifier must be supplied")
  val activityScheduleId: Long?,

  @Schema(
    description = "The past or present date on which the waiting list application was requested",
    example = "2023-06-23",
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  @field:NotNull(message = "Application date must be supplied")
  @field:PastOrPresent(message = "Application date cannot be in the future")
  val applicationDate: LocalDate?,

  @Schema(
    description = "The person who made the request",
    example = "Fred Bloggs",
  )
  @field:NotBlank(message = "Requested by must be supplied")
  @field:Size(max = 100, message = "Requested by must not exceed {max} characters")
  val requestedBy: String?,

  @Schema(
    description = "Any particular comments related to the waiting list request",
    example = "The prisoner has specifically requested to attend this activity",
  )
  @field:Size(max = 500, message = "Comments must not exceed {max} characters")
  val comments: String? = null,

  @Schema(
    description = "The status of the application. Only PENDING, APPROVED or DECLINED are allowed when creating.",
    example = "PENDING",
  )
  @field:NotNull(message = "Status must be supplied")
  val status: WaitingListStatus?,
) {
  @AssertTrue(message = "Only PENDING, APPROVED or DECLINED are allowed for status")
  private fun isStatus() = status == null || listOf(PENDING, APPROVED, DECLINED).contains(status)
}
