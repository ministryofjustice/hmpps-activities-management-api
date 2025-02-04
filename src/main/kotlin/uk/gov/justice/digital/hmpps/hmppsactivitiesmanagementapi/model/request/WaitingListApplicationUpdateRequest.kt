package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import java.time.LocalDate

@Schema(
  description =
  """
  Describes a single waiting list application to be updated.
  """,
)
data class WaitingListApplicationUpdateRequest(

  @Schema(
    description = """
      The updated past or present date on which the waiting list application was requested.
      
      Note this cannot be after the date the waiting list application was first made.
    """,
    example = "2023-06-23",
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val applicationDate: LocalDate? = null,

  @Schema(
    description = "The updated person who made the request",
    example = "Fred Bloggs",
  )
  @field:Size(max = 100, message = "Requested by must not exceed {max} characters")
  val requestedBy: String? = null,

  @Schema(
    description = "The updated comments related to the waiting list request",
    example = "The prisoner has specifically requested to attend this activity",
  )
  @field:Size(max = 500, message = "Comments must not exceed {max} characters")
  val comments: String? = null,

  @Schema(
    description = "The updated status of the application. Only PENDING, APPROVED or DECLINED are allowed when updating.",
    example = "PENDING",
  )
  val status: WaitingListStatus? = null,
) {
  @AssertTrue(message = "Requested by cannot be empty or blank")
  private fun isRequestedBy() = requestedBy == null || requestedBy.isNotBlank()

  @AssertTrue(message = "Application date cannot be in the future")
  private fun isApplicationDate() = applicationDate == null || applicationDate <= LocalDate.now()

  @AssertTrue(message = "Only PENDING, APPROVED or DECLINED are allowed for status")
  private fun isStatus() = status == null || listOf(WaitingListStatus.PENDING, WaitingListStatus.APPROVED, WaitingListStatus.DECLINED).contains(status)
}
