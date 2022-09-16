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
  val attendanceReasonId: Long? = null,

  val code: String,

  val description: String
)
