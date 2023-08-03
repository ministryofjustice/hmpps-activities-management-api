package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "Describes a single waiting list for a prisoner who is waiting to be allocated to an activity.")
data class WaitingList(

  @Schema(
    description = "The internally-generated ID for this waiting list",
    example = "111111",
  )
  val id: Long,

  @Schema(
    description = "The internally-generated ID for the associated activity schedule",
    example = "222222",
  )
  val scheduleId: Long,

  @Schema(
    description = "The internally-generated ID for the associated allocation",
    example = "333333",
  )
  val allocationId: Long? = null,

  @Schema(
    description = "The prison code for this waiting list",
    example = "PVI",
  )
  val prisonCode: String,

  @Schema(
    description = "The prisoner number (NOMIS ID) for this waiting list",
    example = "A1234AA",
  )
  val prisonerNumber: String,

  @Schema(
    description = "The prisoner booking id (NOMIS ID) for this waiting list",
    example = "10001",
  )
  val bookingId: Long,

  @Schema(
    description = "The status of this waiting list",
    example = "PENDING",
  )
  val status: WaitingListStatus,

  @Schema(
    description = "The past or present date on which the waiting list was requested",
    example = "2023-06-23",
  )
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val requestedDate: LocalDate,

  @Schema(
    description = "The person who made the request for this waiting list",
    example = "Fred Bloggs",
  )
  val requestedBy: String,

  @Schema(
    description = "Any particular comments related to this waiting list",
    example = "The prisoner has specifically requested to attend this activity",
  )
  val comments: String? = null,

  @Schema(
    description = "The reason for the waiting list request to be declined (null if status is not declined)",
    example = "The prisoner has specifically requested to attend this activity",
  )
  val declinedReason: String? = null,

  @Schema(
    description = "The date and time the waiting list was first created",
    example = "2023-00-03T12:00:00",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val creationTime: LocalDateTime,

  @Schema(
    description = "The person who created the waiting list i.e the user at the time",
    example = "Jon Doe",
  )
  val createdBy: String,

  @Schema(
    description = "The date and time the waiting list was last updated",
    example = "2023-00-04T16:30:00",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updatedTime: LocalDateTime? = null,

  @Schema(
    description = "The person who last made changes to the waiting list",
    example = "Jane Doe",
  )
  val updatedBy: String? = null,
)
