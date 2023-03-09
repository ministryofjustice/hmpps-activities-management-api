package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason

val sickReason =
  AttendanceReason(
    id = 1,
    code = "SICK",
    description = "Sick",
    attended = false,
    capturePay = true,
    captureMoreDetail = true,
    captureCaseNote = false,
    captureIncentiveLevelWarning = false,
    captureOtherText = false,
    displayInAbsence = true,
    displaySequence = 1,
    notes = "Maps to ACCAB in NOMIS",
  )
val refusedReason =
  AttendanceReason(
    id = 2,
    code = "REFUSED",
    description = "Refused to attend",
    attended = false,
    capturePay = false,
    captureMoreDetail = false,
    captureCaseNote = true,
    captureIncentiveLevelWarning = true,
    captureOtherText = false,
    displayInAbsence = true,
    displaySequence = 2,
    notes = "Maps to UNACAB in NOMIS",
  )
val notRequiredReason =
  AttendanceReason(
    id = 3,
    code = "NREQ",
    description = "Not required or excused",
    attended = false,
    capturePay = false,
    captureMoreDetail = false,
    captureCaseNote = false,
    captureIncentiveLevelWarning = false,
    captureOtherText = false,
    displayInAbsence = true,
    displaySequence = 3,
    notes = "Maps to ACCAB in NOMIS",
  )
val restReason =
  AttendanceReason(
    id = 4,
    code = "REST",
    description = "Rest day",
    attended = false,
    capturePay = true,
    captureMoreDetail = false,
    captureCaseNote = false,
    captureIncentiveLevelWarning = false,
    captureOtherText = false,
    displayInAbsence = true,
    displaySequence = 4,
    notes = "Maps to ACCAB in NOMIS",
  )
val clashReason =
  AttendanceReason(
    id = 5,
    code = "CLASH",
    description = "Prisoner's schedule shows another activity",
    attended = false,
    capturePay = false,
    captureMoreDetail = false,
    captureCaseNote = false,
    captureIncentiveLevelWarning = false,
    captureOtherText = false,
    displayInAbsence = true,
    displaySequence = 5,
    notes = "Maps to ACCAB in NOMIS",
  )
val otherReason =
  AttendanceReason(
    id = 6,
    code = "OTHER",
    description = "Other absence reason not listed",
    attended = false,
    capturePay = true,
    captureMoreDetail = false,
    captureCaseNote = false,
    captureIncentiveLevelWarning = false,
    captureOtherText = true,
    displayInAbsence = true,
    displaySequence = 6,
    notes = "Maps to UNACAB in NOMIS",
  )
val suspendedReason =
  AttendanceReason(
    id = 7,
    code = "SUSP",
    description = "Suspended",
    attended = false,
    capturePay = false,
    captureMoreDetail = false,
    captureCaseNote = false,
    captureIncentiveLevelWarning = false,
    captureOtherText = false,
    displayInAbsence = false,
    displaySequence = null,
    notes = "Maps to ACCAB in NOMIS",
  )
val cancelledReason =
  AttendanceReason(
    id = 8,
    code = "CANC",
    description = "Cancelled",
    attended = false,
    capturePay = false,
    captureMoreDetail = false,
    captureCaseNote = false,
    captureIncentiveLevelWarning = false,
    captureOtherText = false,
    displayInAbsence = false,
    displaySequence = null,
    notes = "Maps to ACCAB in NOMIS",
  )
val attendedReason =
  AttendanceReason(
    id = 9,
    code = "ATT",
    description = "Attended",
    attended = true,
    capturePay = false,
    captureMoreDetail = false,
    captureCaseNote = false,
    captureIncentiveLevelWarning = false,
    captureOtherText = false,
    displayInAbsence = false,
    displaySequence = null,
    notes = "Maps to ATT in NOMIS",
  )
