package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.LocalTime
import java.util.stream.Stream

class AttendanceSyncTest {

  @ParameterizedTest
  @MethodSource("issuePay_reasonCode_reasonDesc_aComment_warning_otherReason_expectedComment_Provider")
  fun `entity is converted to model with different attendance reasons`(
    issuePay: Boolean?,
    reasonCode: String,
    reasonDesc: String,
    aComment: String?,
    warning: Boolean?,
    otherReason: String?,
    expectedComment: String?,
  ) {
    val attendanceSessionDate = LocalDate.now()
    val startTime = LocalTime.now()
    val endTime = startTime.plusHours(1)

    val attendanceSyncEntity = AttendanceSync(
      attendanceId = 1L,
      scheduledInstanceId = 2L,
      activityScheduleId = 3L,
      sessionDate = attendanceSessionDate,
      sessionStartTime = startTime,
      sessionEndTime = endTime,
      prisonerNumber = "ABC123",
      bookingId = 4L,
      attendanceReasonCode = reasonCode,
      comment = aComment,
      status = "COMPLETED",
      payAmount = null,
      bonusAmount = null,
      issuePayment = issuePay,
      attendanceReasonDescription = reasonDesc,
      incentiveLevelWarningIssued = warning,
      otherAbsenceReason = otherReason,
    )

    with(attendanceSyncEntity.toModel()) {
      assertThat(attendanceId).isEqualTo(1)
      assertThat(scheduledInstanceId).isEqualTo(2)
      assertThat(activityScheduleId).isEqualTo(3)
      assertThat(sessionDate).isEqualTo(attendanceSessionDate)
      assertThat(sessionStartTime).isEqualTo(startTime)
      assertThat(sessionEndTime).isEqualTo(endTime)
      assertThat(prisonerNumber).isEqualTo("ABC123")
      assertThat(bookingId).isEqualTo(4)
      assertThat(attendanceReasonCode).isEqualTo(reasonCode)
      assertThat(comment).isEqualTo(expectedComment)
      assertThat(status).isEqualTo("COMPLETED")
      assertThat(payAmount).isEqualTo(null)
      assertThat(bonusAmount).isEqualTo(null)
      assertThat(issuePayment).isEqualTo(issuePay)
    }
  }

  companion object {
    @JvmStatic
    fun issuePay_reasonCode_reasonDesc_aComment_warning_otherReason_expectedComment_Provider(): Stream<Arguments> = Stream.of(
      arguments(null, "CLASH", "Person’s schedule shows another appointment", null, null, null, "Person’s schedule shows another appointment"),
      arguments(true, "SICK", "Sick", "a comment", null, null, "Sick - Paid - a comment"),
      arguments(false, "SICK", "Sick", "a comment", null, null, "Sick - Unpaid - a comment"),
      arguments(true, "SICK", "Sick", null, null, null, "Sick - Paid"),
      arguments(false, "SICK", "Sick", "", null, null, "Sick - Unpaid"),
      arguments(true, "OTHER", "Other: absence reason not listed", "a comment", null, "other absence comment", "Other - Paid - other absence comment"),
      arguments(false, "OTHER", "Other: absence reason not listed", "a comment", null, "other absence comment", "Other - Unpaid - other absence comment"),
      arguments(true, "OTHER", "Other: absence reason not listed", null, null, null, "Other - Paid"),
      arguments(false, "OTHER", "Other: absence reason not listed", null, null, null, "Other - Unpaid"),
      arguments(null, "REFUSED", "Other: absence reason not listed", "a comment", true, null, "Incentive level warning issued - "),
      arguments(null, "REFUSED", "Other: absence reason not listed", "a comment", false, null, ""),
      arguments(null, "NOT_REQUIRED", "Not required or excused", "a comment", null, null, "Not required or excused"),
      arguments(true, "REST", "Rest day", "a comment", null, null, "Rest day - Paid"),
      arguments(false, "REST", "Rest day", "a comment", null, null, "Rest day - Unpaid"),
      arguments(true, "SUSPENDED", "Suspended", "a comment", null, null, "Suspended - Paid"),
      arguments(false, "SUSPENDED", "Suspended", "a comment", null, null, "Suspended - Unpaid"),
      arguments(null, "AUTO_SUSPENDED", "Temporarily absent", "a comment", null, null, "Temporarily absent from prison - Unpaid"),
      arguments(true, "CANCELLED", "Not attended - Session cancelled", "a comment", null, null, "Activity cancelled - Paid - Not attended - Session cancelled - a comment"),
      arguments(true, "CANCELLED", "Not attended - Session cancelled", null, null, null, "Activity cancelled - Paid - Not attended - Session cancelled"),
      arguments(true, "ATTENDED", "Attended", null, null, null, null),
    )
  }
}
