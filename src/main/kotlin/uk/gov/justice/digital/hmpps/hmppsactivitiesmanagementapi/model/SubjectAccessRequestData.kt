package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarAllocation as EntitySarAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarAppointment as EntitySarAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarWaitingList as EntitySarWaitingList

@Schema(
  description = """
    Describes Subject Access Request Data
  """,
)
data class SubjectAccessRequestData(
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

  @Schema(description = "All of the attendances for the prisoner for the period")
  val attendanceSummary: List<SarAttendanceSummary>,

  @Schema(description = "Waiting list applications for a prisoner")
  val waitingListApplications: List<SarWaitingList>,

  @Schema(description = "All of the appointments for the prisoner for the period")
  val appointments: List<SarAppointment>,
)

data class SarAllocation(
  @Schema(description = "The internally-generated ID for this allocation", example = "123456")
  val allocationId: Long,

  @Schema(description = "The prison code where this activity takes place", example = "PVI")
  val prisonCode: String,

  @Schema(description = "The status of the allocation", example = "Active")
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
  val payBand: String? = null,

  @Schema(description = "The date the allocation entry was created", example = "2022-01-01")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val createdDate: LocalDate,

  @Schema(description = "The category name for this activity", example = "Gym, sport, fitness")
  val activityCategoryName: String,

  @Schema(description = "The category description for this activity", example = "Such as sports clubs, like football, or recreational gym sessions")
  val activityCategoryDescription: String,

  @Schema(description = "Flag to indicate if attendance is required for this activity", example = "true")
  val attendanceRequired: Boolean,

  @Schema(description = "Whether the activity is a paid activity", example = "true")
  val paid: Boolean,

  @Schema(description = "Flag to indicate if the activity is carried out outside of the prison", example = "false")
  var outsideWork: Boolean,

  @Schema(description = "The most recent risk assessment level for this activity", example = "high")
  val riskLevel: String,

  @Schema(description = "The organiser of the activity, can be null", example = "Prison staff")
  val organiser: String? = null,

  @Schema(description = "The DPS location id of the activity, can be null", example = "4475b5d5-873c-4f88-a5b7-2d20e9224a62")
  val dpsLocationId: UUID? = null,

  @Schema(description = "Is the activity location in cell or not?", example = "false")
  val inCell: Boolean = false,

  @Schema(description = "Is the activity location off wing or not?", example = "false")
  val offWing: Boolean = false,

  @Schema(description = "Is the activity location on wing or not?", example = "true")
  val onWing: Boolean = false,
) {
  constructor(allocation: EntitySarAllocation) : this(
    allocation.allocationId,
    allocation.prisonCode,
    allocation.prisonerStatus.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
    allocation.startDate,
    allocation.endDate,
    allocation.activityId,
    allocation.activitySummary,
    allocation.payBand,
    allocation.createdDate,
    allocation.activityCategoryName,
    allocation.activityCategoryDescription,
    allocation.attendanceRequired,
    allocation.paid,
    allocation.outsideWork,
    allocation.riskLevel,
    allocation.organiser,
    allocation.dpsLocationId,
    allocation.inCell,
    allocation.offWing,
    allocation.onWing,
  )
}

data class SarAttendanceSummary(
  @Schema(description = "The summary reason for a recorded prisoner attendance", example = "Attended")
  val attendanceReason: String,

  @Schema(description = "A count of attendance for a given reason", example = "3")
  val count: Int,
)

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

  @Schema(description = "The identity of the requester of the activity", example = "Prison staff")
  val originator: String,

  @Schema(description = "The status of the waiting list entry", example = "Active")
  val status: String,

  @Schema(description = "The date the waiting list entry was last updated, can be null", example = "2022-01-01")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val statusDate: LocalDate?,

  @Schema(description = "The comments associated with this waiting list entry, can be null", example = "OK to proceed")
  val comments: String?,

  @Schema(description = "The date the waiting list entry was created", example = "2022-01-01")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val createdDate: LocalDate,

  @Schema(description = "The reason for the waiting list request to be declined, can be null", example = "The prisoner has specifically requested to attend this activity")
  val declinedReason: String? = null,

  @Schema(description = "The category name for this activity", example = "Gym, sport, fitness")
  val activityCategoryName: String,

  @Schema(description = "The category description for this activity", example = "Such as sports clubs, like football, or recreational gym sessions")
  val activityCategoryDescription: String,

  @Schema(description = "Flag to indicate if attendance is required for this activity", example = "true")
  val attendanceRequired: Boolean,

  @Schema(description = "Whether the activity is a paid activity", example = "true")
  val paid: Boolean,

  @Schema(description = "Flag to indicate if the activity is carried out outside of the prison", example = "false")
  var outsideWork: Boolean,

  @Schema(description = "The most recent risk assessment level for this activity", example = "high")
  val riskLevel: String,

  @Schema(description = "The organiser of the activity, can be null", example = "Prison staff")
  val organiser: String? = null,

  @Schema(description = "The DPS location id of the activity, can be null", example = "4475b5d5-873c-4f88-a5b7-2d20e9224a62")
  val dpsLocationId: UUID? = null,

  @Schema(description = "Is the activity location in cell or not?", example = "false")
  val inCell: Boolean = false,

  @Schema(description = "Is the activity location off wing or not?", example = "false")
  val offWing: Boolean = false,

  @Schema(description = "Is the activity location on wing or not?", example = "true")
  val onWing: Boolean = false,
) {
  constructor(waitingList: EntitySarWaitingList) : this(
    waitingList.waitingListId,
    waitingList.prisonCode,
    waitingList.activitySummary,
    waitingList.applicationDate,
    waitingList.originator.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
    waitingList.status.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) },
    waitingList.statusDate,
    waitingList.comments,
    waitingList.createdDate,
    waitingList.declinedReason,
    waitingList.activityCategoryName,
    waitingList.activityCategoryDescription,
    waitingList.attendanceRequired,
    waitingList.paid,
    waitingList.outsideWork,
    waitingList.riskLevel,
    waitingList.organiser,
    waitingList.dpsLocationId,
    waitingList.inCell,
    waitingList.offWing,
    waitingList.onWing,
  )
}

data class SarAppointment(
  @Schema(description = "The internally-generated ID for this appointment", example = "123456")
  val appointmentId: Long,

  @Schema(description = "The prison code where this appointment takes place", example = "PVI")
  val prisonCode: String,

  @Schema(description = "The category of the appointment", example = "Education")
  val category: String,

  @Schema(description = "The date of the appointment", example = "2022-01-01")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val date: LocalDate,

  @Schema(description = "The start time of the appointment", example = "12:30")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The end time of the appointment, can be null", example = "10:15")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(description = "Any extra information about the appointment, can be null", example = "Discuss God")
  val extraInformation: String?,

  @Schema(description = "Any extra information for the prisoner attending ", example = "Please arrive 10 minutes early")
  val prisonerExtraInformation: String? = null,

  @Schema(description = "The attendance of the appointment", allowableValues = ["Yes", "No", "Unmarked"], example = "Yes")
  val attended: String,

  @Schema(description = "The date the appointment entry was created", example = "2022-01-01")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val createdDate: LocalDate,

  @Schema(description = "The custom name of the appointment, can be null", example = "Gym Activity")
  val customName: String? = null,

  @Schema(description = "The organiser of the appointment, can be null", example = "Prison staff")
  val organiser: String? = null,

  @Schema(description = "The DPS location id of the appointment, can be null", example = "4475b5d5-873c-4f88-a5b7-2d20e9224a62")
  val dpsLocationId: UUID? = null,

  @Schema(description = "Is the appointment location in cell or not?", example = "false")
  val inCell: Boolean = false,

  @Schema(description = "Is the appointment location off wing or not?", example = "false")
  val offWing: Boolean = false,

  @Schema(description = "Is the appointment location on wing or not?", example = "true")
  val onWing: Boolean = false,

  @Schema(description = "The reason the appointment was cancelled, can be null", example = "Created in error")
  val cancellationReason: String? = null,

  @Schema(description = "Who cancelled the appointment, can be null", example = "ABC12D")
  val cancelledBy: String? = null,
) {
  constructor(appointment: EntitySarAppointment) : this(
    appointment.appointmentId,
    appointment.prisonCode,
    appointment.category,
    appointment.startDate,
    appointment.startTime,
    appointment.endTime,
    appointment.extraInformation,
    appointment.prisonerExtraInformation,
    appointment.attended,
    appointment.createdDate,
    appointment.customName,
    appointment.organiser,
    appointment.dpsLocationId,
    appointment.inCell,
    appointment.offWing,
    appointment.onWing,
    appointment.cancellationReason,
    appointment.cancelledBy,
  )
}
