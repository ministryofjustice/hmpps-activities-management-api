package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarAllocation as EntitySarAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarWaitingList as EntitySarWaitingList

data class SubjectAccessRequestContent(
  @Schema(description = "The prisoner number (Nomis ID)", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The from date for the request", example = "2022-01-01")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val fromDate: LocalDate,

  @Schema(description = "The to date for the request", example = "2024-01-01")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val toDate: LocalDate,

  @Schema(description = "All of the allocations for the prisoner for the period")
  val allocations: List<SarAllocation>,

  @Schema(description = "Waiting list application for a prisoner")
  val waitingListApplications: List<SarWaitingList>,
)

data class SarAllocation(
  @Schema(description = "The internally-generated ID for this allocation", example = "123456")
  val allocationId: Long,

  @Schema(description = "The prison code where this activity takes place", example = "PVI")
  val prisonCode: String,

  @Schema(description = "The status of the allocation", example = "ACTIVE")
  val prisonerStatus: String,

  @Schema(description = "The start date of the allocation", example = "2022-01-01")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(description = "The end date of the allocation, can be null", example = "2024-01-01")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val endDate: LocalDate?,

  @Schema(description = "The internally-generated ID for this activity", example = "123456")
  val activityId: Long,

  @Schema(description = "A brief summary description of this activity", example = "Maths level 1")
  val activitySummary: String,

  @Schema(description = "The pay band for the allocation, can be null e.g. unpaid activity", example = "Pay band 1 (lowest)")
  val payBand: String?,
) {
  constructor(allocation: EntitySarAllocation) : this(
    allocation.allocationId,
    allocation.prisonCode,
    allocation.prisonerStatus,
    allocation.startDate,
    allocation.endDate,
    allocation.activityId,
    allocation.activitySummary,
    allocation.payBand,
  )
}

data class SarWaitingList(
  @Schema(description = "The internally-generated ID for this waiting list entry", example = "123456")
  val waitingListId: Long,

  @Schema(description = "The prison code where this activity takes place", example = "PVI")
  val prisonCode: String,

  @Schema(description = "A brief summary description of this activity", example = "Maths level 1")
  val activitySummary: String,

  @Schema(description = "The date the application was added to the waiting list entry", example = "2022-01-01")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val applicationDate: LocalDate,

  @Schema(description = "The identity of the requester of the activity", example = "2022-01-01")
  val originator: String,

  @Schema(description = "The status of the waiting list entry", example = "ACTIVE")
  val status: String,

  @Schema(description = "The date the waiting list entry was last updated, can be null", example = "2022-01-01")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val statusDate: LocalDate?,

  @Schema(description = "The comments associated with this waiting list entry, can be null", example = "ACTIVE")
  val comments: String?,

  @Schema(description = "The date the waiting list entry was created", example = "2022-01-01")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val createdDate: LocalDate,

) {
  constructor(waitingList: EntitySarWaitingList) : this(
    waitingList.waitingListId,
    waitingList.prisonCode,
    waitingList.activitySummary,
    waitingList.applicationDate,
    waitingList.originator,
    waitingList.status,
    waitingList.statusDate,
    waitingList.comments,
    waitingList.createdDate,
  )
}
