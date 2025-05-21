package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "advance_attendance_history")
data class AdvanceAttendanceHistory(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val advanceAttendanceHistoryId: Long = 0,

  @ManyToOne
  @JoinColumn(name = "advance_attendance_id", nullable = false)
  val advanceAttendance: AdvanceAttendance,

  var issuePayment: Boolean,

  var recordedTime: LocalDateTime,

  var recordedBy: String,
)
