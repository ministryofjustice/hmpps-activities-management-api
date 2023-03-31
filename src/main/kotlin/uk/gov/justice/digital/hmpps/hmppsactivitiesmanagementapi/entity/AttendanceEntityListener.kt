package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.PreUpdate
import org.springframework.stereotype.Component

@Component
class AttendanceEntityListener {

  @PreUpdate
  fun onUpdate(attendance: Attendance) {
    if (attendance.status != AttendanceStatus.WAITING) {
      attendance.addHistory(
        AttendanceHistory(
          attendance = attendance,
          attendanceReason = attendance.attendanceReason,
          comment = attendance.comment,
          recordedTime = attendance.recordedTime!!,
          recordedBy = attendance.recordedBy!!,
          issuePayment = attendance.issuePayment,
          caseNoteId = attendance.caseNoteId,
          incentiveLevelWarningIssued = attendance.incentiveLevelWarningIssued,
          otherAbsenceReason = attendance.otherAbsenceReason,
        )
      )
    }
  }
}
