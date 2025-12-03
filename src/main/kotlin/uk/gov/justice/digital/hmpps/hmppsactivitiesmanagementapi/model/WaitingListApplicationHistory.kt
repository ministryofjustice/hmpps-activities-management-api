package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import java.time.LocalDate

@Schema(description = "Represents a historical snapshot of a waiting list application for a given ID.")
data class WaitingListApplicationHistory(
  @Schema(
    description = "The internally-generated ID for this waiting list",
    example = "111111",
  )
  val id: Long? = null,

  @Schema(
    description = "The status of this waiting list",
    example = "PENDING",
  )
  val status: WaitingListStatus? = null,

  @Schema(
    description = "The date of application for this waiting list",
    example = "2023-06-23",
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val applicationDate: LocalDate? = null,

  @Schema(
    description = "The person who made the request for this waiting list",
    example = "Fred Bloggs",
  )
  val requestedBy: String? = null,

  @Schema(
    description = "Any particular comments related to this waiting list",
    example = "The prisoner has specifically requested to attend this activity",
  )
  val comments: String? = null,

  @Schema(
    description = "The internally generated ID for this revision",
  )
  val revisionId: Long,

  @Schema(
    description = "The value for the type of revision: 0 - ADD, 1 - MOD, 2 - DEL",
  )
  val revisionType: String,
)
