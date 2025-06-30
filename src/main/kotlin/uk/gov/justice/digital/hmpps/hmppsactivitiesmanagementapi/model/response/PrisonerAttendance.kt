package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason
import java.time.LocalDateTime

data class PrisonerAttendance(
  val id: Long,
  val scheduledInstanceId: Long,
  val prisonerNumber: String,
  val attendanceReason: AttendanceReason? = null,
  val comment: String? = null,
  val recordedTime: LocalDateTime? = null,
  val recordedBy: String? = null,
  val status: AttendanceStatus,
  val payAmount: Int? = null,
  val bonusAmount: Int? = null,
  val pieces: Int? = null,
  val initialIssuePayment: Boolean? = null,
  val incentiveLevelWarningIssued: Boolean? = null,
  val otherAbsenceReason: String? = null,
  val attendanceHistory: List<AttendanceHistory>? = null,
)
