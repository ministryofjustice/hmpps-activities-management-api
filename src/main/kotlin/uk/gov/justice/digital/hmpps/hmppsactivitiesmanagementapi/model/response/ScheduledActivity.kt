package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class ScheduledActivity(
  val scheduledInstanceId: Long,

  val allocationId: Long,

  val prisonCode: String,

  @JsonFormat(pattern = "yyyy-MM-dd")
  val sessionDate: LocalDate,

  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime? = null,

  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime? = null,

  val prisonerNumber: String,

  val bookingId: Long,

  val inCell: Boolean,

  val onWing: Boolean,

  val offWing: Boolean,

  val internalLocationId: Int? = null,

  val dpsLocationId: UUID? = null,

  val internalLocationCode: String? = null,

  val internalLocationDescription: String? = null,

  val scheduleDescription: String? = null,

  val activityId: Int,

  val activityCategory: String,

  val activitySummary: String? = null,

  val cancelled: Boolean = false,

  val suspended: Boolean = false,

  val autoSuspended: Boolean = false,

  val timeSlot: TimeSlot,

  val issuePayment: Boolean?,

  val attendanceStatus: AttendanceStatus?,

  val attendanceReasonCode: AttendanceReasonEnum?,

  val paidActivity: Boolean,

  val possibleAdvanceAttendance: Boolean,
)
