package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "attendance")
data class Attendance(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val attendanceId: Int? = null,

  @ManyToOne
  @JoinColumn(name = "activity_instance_id", nullable = false)
  val activityInstance: ActivityInstance,

  val prisonCode: String,

  val prisonerNumber: String,

  @OneToOne
  @JoinColumn(name = "attendance_reason_id", nullable = false)
  val attendanceReason: AttendanceReason,

  var comment: String? = null,

  val posted: Boolean,

  val recordedAt: LocalDateTime,

  val recordedBy: String,

  var status: String? = null,

  var payAmount: Int? = null,

  var bonusAmount: Int? = null,

  var pieces: Int? = null
)
