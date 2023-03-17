package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReason

internal fun attendanceReasonEntities() =
  listOf(
    AttendanceReason(
      attendanceReasonId = 1,
      code = "reason code",
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
