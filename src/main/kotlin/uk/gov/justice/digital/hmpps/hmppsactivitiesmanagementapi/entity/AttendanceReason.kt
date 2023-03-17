package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason as modelAttendanceReason

@Entity
@Table(name = "attendance_reason")
data class AttendanceReason(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val attendanceReasonId: Long = -1,

  val code: String,

  val description: String,

  val attended: Boolean,

  val capturePay: Boolean,

  val captureMoreDetail: Boolean,

  val captureCaseNote: Boolean,

  val captureIncentiveLevelWarning: Boolean,

  val captureOtherText: Boolean,

  val displayInAbsence: Boolean,

  val displaySequence: Int?,

  val notes: String,
) {

  @Override
  override fun toString(): String {
    return this::class.simpleName + "(attendanceReasonId = $attendanceReasonId )"
  }

  fun toModel() = modelAttendanceReason(
    id = attendanceReasonId,
    code = code,
    description = description,
    attended = attended,
    capturePay = capturePay,
    captureMoreDetail = captureMoreDetail,
    captureCaseNote = captureCaseNote,
    captureIncentiveLevelWarning = captureIncentiveLevelWarning,
    captureOtherText = captureOtherText,
    displayInAbsence = displayInAbsence,
    displaySequence = displaySequence,
    notes = notes,
  )
}

fun List<AttendanceReason>.toModel() = map { it.toModel() }
