package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "attendance_reason")
data class AttendanceReason(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val attendanceReasonId: Long = -1,

  val code: String,

  val description: String,
) {

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(attendanceReasonId = $attendanceReasonId )"
  }
}
