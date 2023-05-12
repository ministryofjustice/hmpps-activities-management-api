package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AttendanceTest {
  private val activity = activityEntity()
  private val activitySchedule = activity.schedules().first()
  private val instance = activitySchedule.instances().first()
  private val attendance = instance.attendances.first()
  private val today = LocalDateTime.now()

  @Test
  fun `uncancel method sets the state correctly`() {
    val attendance = Attendance(
      scheduledInstance = mock(),
      prisonerNumber = "P000111",
      attendanceReason = attendanceReasons()["ATTENDED"]!!,
      status = AttendanceStatus.COMPLETED,
      comment = "Some Comment",
      recordedBy = "Old User",
      recordedTime = LocalDateTime.now(),
    )

    attendance.uncancel()

    with(attendance) {
      assertThat(attendanceReason).isNull()
      assertThat(status).isEqualTo(AttendanceStatus.WAITING)
      assertThat(comment).isNull()
      assertThat(recordedBy).isNull()
      assertThat(recordedTime).isNull()
      assertThat(otherAbsenceReason).isNull()
    }
  }

  @Test
  fun `can cancel attendance`() {
    val attendanceReason = attendanceReasons()["CANCELLED"]!!
    val canceledInstance = instance.copy(cancelledBy = "USER1", cancelledTime = today, cancelledReason = "Staff unavailable")
    val attendanceWithCanceledInstance = attendance.copy(scheduledInstance = canceledInstance)

    attendanceWithCanceledInstance.cancel(attendanceReason)
    assertThat(attendanceWithCanceledInstance.status).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendanceWithCanceledInstance.issuePayment).isEqualTo(true)
    assertThat(attendanceWithCanceledInstance.attendanceReason).isEqualTo(attendanceReason)
    assertThat(attendanceWithCanceledInstance.comment).isEqualTo("Staff unavailable")
    assertThat(attendanceWithCanceledInstance.recordedTime).isCloseTo(LocalDateTime.now(), Assertions.within(1, ChronoUnit.SECONDS))
    assertThat(attendanceWithCanceledInstance.recordedBy).isEqualTo("USER1")
  }

  @Test
  fun `marking attendance records history`() {
    val attendanceReason = attendanceReasons()["OTHER"]!!
    val otherAttendance = attendance.copy(
      status = AttendanceStatus.COMPLETED,
      attendanceReason = attendanceReason,
      otherAbsenceReason = "Other reason",
      comment = "Some comment",
      issuePayment = true,
    )

    otherAttendance.mark(
      principalName = "New user",
      reason = null,
      newStatus = AttendanceStatus.WAITING,
      newComment = null,
      newIssuePayment = null,
      newIncentiveLevelWarningIssued = null,
      newCaseNoteId = null,
      newOtherAbsenceReason = null,
    )

    with(otherAttendance.history().last()) {
      assertThat(this.attendanceReason).isEqualTo(attendanceReason)
      assertThat(this.comment).isEqualTo("Some comment")
      assertThat(this.recordedBy).isEqualTo("Joe Bloggs")
      assertThat(this.issuePayment).isEqualTo(true)
      assertThat(this.otherAbsenceReason).isEqualTo("Other reason")
    }
  }
}
