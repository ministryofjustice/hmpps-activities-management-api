package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum

internal fun attendanceReasonEntities() = listOf(
  AttendanceReason(
    attendanceReasonId = 1,
    code = AttendanceReasonEnum.ATTENDED,
    description = "reason description",
    attended = false,
    capturePay = true,
    captureMoreDetail = true,
    captureCaseNote = false,
    captureIncentiveLevelWarning = false,
    captureOtherText = false,
    displayInAbsence = true,
    displaySequence = 1,
    notes = "reason notes",
  ),
)
