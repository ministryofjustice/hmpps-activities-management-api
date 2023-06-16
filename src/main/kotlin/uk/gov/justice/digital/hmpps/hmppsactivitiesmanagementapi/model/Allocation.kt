package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description = "A prisoner who is allocated to an activity")
data class Allocation(

  @Schema(description = "The internally-generated ID for this allocation", example = "123456")
  val id: Long,

  @Schema(description = "The prisoner number (Nomis ID)", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The offender booking id", example = "10001")
  val bookingId: Long,

  val activitySummary: String,

  val scheduleId: Long,

  val scheduleDescription: String,

  @Schema(description = "Indicates whether this allocation is to an activity within the 'Not in work' category")
  val isUnemployment: Boolean = false,

  @Schema(description = "The activity pay assigned to this allocation")
  val payRate: ActivityPay? = null,

  @Schema(description = "The date when the prisoner will start the activity", example = "2022-09-10")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(description = "The date when the prisoner will stop attending the activity", example = "2023-09-10")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val endDate: LocalDate? = null,

  @Schema(description = "The date and time the prisoner was allocated to the activity", example = "2022-09-01T09:00:00")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val allocatedTime: LocalDateTime? = null,

  @Schema(description = "The person who allocated the prisoner to the activity", example = "Mr Blogs")
  val allocatedBy: String? = null,

  @Schema(
    description = "The date and time the prisoner was deallocated from the activity",
    example = "2022-09-02T09:00:00",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val deallocatedTime: LocalDateTime? = null,

  @Schema(description = "The person who deallocated the prisoner from the activity", example = "Mrs Blogs")
  val deallocatedBy: String? = null,

  val deallocatedReason: DeallocationReason? = null,

  @Schema(description = "The date and time the allocation was suspended", example = "2022-09-02T09:00:00")
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val suspendedTime: LocalDateTime? = null,

  @Schema(description = "The person who suspended the prisoner from the activity", example = "Mrs Blogs")
  val suspendedBy: String? = null,

  @Schema(
    description = "The descriptive reason why this prisoner was suspended from the activity",
    example = "Temporarily released from prison",
  )
  val suspendedReason: String? = null,

  @Schema(description = "The status of the allocation", example = "ACTIVE")
  val status: PrisonerStatus,
)

@Schema(
  description = "The code and descriptive reason why this prisoner was deallocated from the activity",
)
data class DeallocationReason(
  @Schema(
    description = "The code for the deallocation reason",
    example = "RELEASED",
  )
  val code: String,

  @Schema(
    description = "The description for the deallocation reason",
    example = "Released from prison",
  )
  val description: String,
)
