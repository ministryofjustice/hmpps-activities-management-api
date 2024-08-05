package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.FutureOrPresent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import java.time.LocalDate

@Schema(description = "Describes an allocation to be updated")
data class AllocationUpdateRequest(

  @Schema(description = "The date when the prisoner will start the activity", example = "2022-09-10")
  @JsonFormat(pattern = "yyyy-MM-dd")
  @field:FutureOrPresent(message = "Start date must not be in the past")
  val startDate: LocalDate? = null,

  @Schema(description = "The date when the prisoner will stop attending the activity", example = "2023-09-10")
  @JsonFormat(pattern = "yyyy-MM-dd")
  @field:FutureOrPresent(message = "End date must not be in the past")
  val endDate: LocalDate? = null,

  @Schema(description = "A flag to indicate that the allocation end date is to be removed", example = "true")
  val removeEndDate: Boolean? = null,

  @Schema(
    description = "The reason code for the deallocation",
    example = "RELEASED",
    allowableValues = ["OTHER", "PERSONAL", "PROBLEM", "REMOVED", "SECURITY", "UNACCEPTABLE_ATTENDANCE", "UNACCEPTABLE_BEHAVIOUR", "WITHDRAWN"],
  )
  val reasonCode: String? = null,

  @Schema(description = "Where a prison uses pay bands to differentiate earnings, this is the pay band given to this prisoner")
  val payBandId: Long? = null,

  @Schema(
    description = "The days and times that the prisoner is excluded from this activity's schedule. " +
      "All values must match a slot where the activity is scheduled to run, and due to sync to nomis, " +
      "there can not not be exclusions defined on the same day and time slot over multiple weeks.",
  )
  val exclusions: List<Slot>? = null,

  @Schema(description = "The scheduled instance id required when allocation starts today")
  val scheduleInstanceId: Long? = null,
)
