package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
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
  val attendanceReasonId: Long = 0,

  @Enumerated(EnumType.STRING)
  val code: AttendanceReasonEnum,

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
  override fun toString(): String = this::class.simpleName + "(attendanceReasonId = $attendanceReasonId )"

  fun toModel() = modelAttendanceReason(
    id = attendanceReasonId,
    code = code.toString(),
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

enum class AttendanceReasonEnum {
  SICK,
  REFUSED,
  NOT_REQUIRED,
  REST,
  CLASH,
  OTHER,
  AUTO_SUSPENDED,
  SUSPENDED,
  CANCELLED,
  ATTENDED,
}
