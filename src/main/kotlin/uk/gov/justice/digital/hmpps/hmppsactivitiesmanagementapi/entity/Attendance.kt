package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.LocalDateTime
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

@Entity
@Table(name = "attendance")
data class Attendance(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val attendanceId: Int = -1,

  @ManyToOne
  @JoinColumn(name = "activity_instance_id", nullable = false)
  val activityInstance: ActivityInstance,

  @ManyToOne
  @JoinColumn(name = "rollout_prison_id", nullable = false)
  val rolloutPrison: RolloutPrison,

  val prisonerNumber: String,

  @ManyToOne
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
