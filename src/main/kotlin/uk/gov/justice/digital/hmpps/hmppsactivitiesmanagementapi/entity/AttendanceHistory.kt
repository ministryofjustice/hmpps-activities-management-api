package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReason
import java.time.LocalDateTime

@Entity
@Table(name = "attendance_history")
data class AttendanceHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val attendanceHistoryId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "attendance_id", nullable = false)
  val attendance: Attendance,

  @OneToOne
  @JoinColumn(name = "attendance_reason_id", nullable = true)
  var attendanceReason: AttendanceReason? = null,

  var comment: String? = null,

  var recordedTime: LocalDateTime,

  var recordedBy: String,

  var issuePayment: Boolean? = null,

  var caseNoteId: Long? = null,

  var incentiveLevelWarningIssued: Boolean? = null,

  var otherAbsenceReason: String? = null,
)
