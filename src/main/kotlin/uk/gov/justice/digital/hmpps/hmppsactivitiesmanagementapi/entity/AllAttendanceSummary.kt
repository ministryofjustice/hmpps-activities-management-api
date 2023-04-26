package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AllAttendanceSummary as AllAttendanceSummaryModel

@Entity
@Immutable
@Table(name = "v_all_attendance_summary")
data class AllAttendanceSummary(
  @Id
  val id: Long,

  val sessionDate: LocalDate,

  val timeSlot: String,

  val status: String,

  val attendanceReasonCode: String?,

  val issuePayment: Boolean?,

  val attendanceCount: Int,
) {
  fun toModel() =
    AllAttendanceSummaryModel(
      id = id,
      sessionDate = sessionDate,
      timeSlot = timeSlot,
      status = status,
      attendanceReasonCode = attendanceReasonCode,
      issuePayment = issuePayment,
      attendanceCount = attendanceCount,
    )
}

fun List<AllAttendanceSummary>.toModel() = map { it.toModel() }
