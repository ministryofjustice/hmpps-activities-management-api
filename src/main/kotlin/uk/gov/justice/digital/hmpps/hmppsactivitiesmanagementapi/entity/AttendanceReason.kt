package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "attendance_reason")
data class AttendanceReason(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val attendanceReasonId: Long = -1,

  val code: String,

  val description: String
) {

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(attendanceReasonId = $attendanceReasonId )"
  }
}
