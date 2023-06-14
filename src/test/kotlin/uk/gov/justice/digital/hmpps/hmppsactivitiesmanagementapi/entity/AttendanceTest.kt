package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import java.time.LocalDate
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
      assertThat(status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(comment).isNull()
      assertThat(recordedBy).isNull()
      assertThat(recordedTime).isNull()
      assertThat(otherAbsenceReason).isNull()
    }
  }

  @Test
  fun `uncancel does not alter the attendance reason of a suspended prisoner`() {
    val attendance = Attendance(
      scheduledInstance = mock(),
      prisonerNumber = "P000111",
      attendanceReason = attendanceReasons()["SUSPENDED"]!!,
      status = AttendanceStatus.COMPLETED,
      comment = "Some Comment",
      recordedBy = "Old User",
      recordedTime = LocalDateTime.now(),
    )

    attendance.uncancel()

    with(attendance) {
      assertThat(attendanceReason).isEqualTo(attendanceReasons()["SUSPENDED"])
      assertThat(status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(comment).isNull()
      assertThat(recordedBy).isNull()
      assertThat(recordedTime).isNull()
      assertThat(otherAbsenceReason).isNull()
    }
  }

  @Test
  fun `can cancel attendance`() {
    val attendanceReason = attendanceReasons()["CANCELLED"]!!
    val canceledInstance =
      instance.copy(cancelledBy = "USER1", cancelledTime = today, cancelledReason = "Staff unavailable")
    val attendanceWithCanceledInstance = attendance.copy(scheduledInstance = canceledInstance)

    attendanceWithCanceledInstance.cancel(attendanceReason)
    assertThat(attendanceWithCanceledInstance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendanceWithCanceledInstance.issuePayment).isEqualTo(true)
    assertThat(attendanceWithCanceledInstance.attendanceReason).isEqualTo(attendanceReason)
    assertThat(attendanceWithCanceledInstance.comment).isEqualTo("Staff unavailable")
    assertThat(attendanceWithCanceledInstance.recordedTime).isCloseTo(
      LocalDateTime.now(),
      Assertions.within(1, ChronoUnit.SECONDS),
    )
    assertThat(attendanceWithCanceledInstance.recordedBy).isEqualTo("USER1")
  }

  @Test
  fun `cancel does not alter the attendance reason of a suspended prisoner`() {
    val attendanceReason = attendanceReasons()["CANCELLED"]!!
    val canceledInstance =
      instance.copy(cancelledBy = "USER1", cancelledTime = today, cancelledReason = "Staff unavailable")
    val attendanceWithCanceledInstance = attendance.copy(attendanceReason = attendanceReasons()["SUSPENDED"], scheduledInstance = canceledInstance)

    attendanceWithCanceledInstance.cancel(attendanceReason)
    assertThat(attendanceWithCanceledInstance.status()).isEqualTo(AttendanceStatus.COMPLETED)
    assertThat(attendanceWithCanceledInstance.issuePayment).isEqualTo(true)
    assertThat(attendanceWithCanceledInstance.attendanceReason).isEqualTo(attendanceReasons()["SUSPENDED"])
    assertThat(attendanceWithCanceledInstance.comment).isEqualTo("Staff unavailable")
    assertThat(attendanceWithCanceledInstance.recordedTime).isCloseTo(
      LocalDateTime.now(),
      Assertions.within(1, ChronoUnit.SECONDS),
    )
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

  @Test
  fun `attendance is editable - WAITING, session was today`() {
    val instanceToday = instance.copy(sessionDate = LocalDate.now())
    val attendance = Attendance(
      scheduledInstance = instanceToday,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.WAITING,
    )
    assertThat(attendance.editable()).isTrue
  }

  @Test
  fun `attendance is editable - WAITING, session was 10 days ago`() {
    val instanceTenDaysAgo = instance.copy(sessionDate = LocalDate.now().minusDays(10))
    val attendance = Attendance(
      scheduledInstance = instanceTenDaysAgo,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.WAITING,
    )
    assertThat(attendance.editable()).isTrue
  }

  @Test
  fun `attendance is editable - COMPLETED, paid, session was today`() {
    val instanceToday = instance.copy(sessionDate = LocalDate.now())
    val attendance = Attendance(
      scheduledInstance = instanceToday,
      issuePayment = true,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.COMPLETED,
      recordedTime = LocalDateTime.now(),
    )
    assertThat(attendance.editable()).isTrue
  }

  @Test
  fun `attendance is editable - COMPLETED, paid, session 3 days ago, but was marked today`() {
    val instanceThreeDaysAgo = instance.copy(sessionDate = LocalDate.now().minusDays(3))
    val attendance = Attendance(
      scheduledInstance = instanceThreeDaysAgo,
      issuePayment = true,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.COMPLETED,
      recordedTime = LocalDateTime.now(),
    )
    assertThat(attendance.editable()).isTrue
  }

  @Test
  fun `attendance is editable - COMPLETED, unpaid, session was 10 days ago`() {
    val instanceTenDaysAgo = instance.copy(sessionDate = LocalDate.now().minusDays(10))
    val attendance = Attendance(
      scheduledInstance = instanceTenDaysAgo,
      issuePayment = false,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.COMPLETED,
      recordedTime = LocalDateTime.now().minusDays(10),
    )
    assertThat(attendance.editable()).isTrue
  }

  @Test
  fun `attendance is NOT editable - WAITING, session was 15 days ago`() {
    val instanceFifteenDaysAgo = instance.copy(sessionDate = LocalDate.now().minusDays(15))
    val attendance = Attendance(
      scheduledInstance = instanceFifteenDaysAgo,
      issuePayment = false,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.WAITING,
    )
    assertThat(attendance.editable()).isFalse
  }

  @Test
  fun `attendance is NOT editable - COMPLETED, paid, session was yesterday and was marked yesterday`() {
    val instanceYesterday = instance.copy(sessionDate = LocalDate.now().minusDays(1))
    val attendance = Attendance(
      scheduledInstance = instanceYesterday,
      issuePayment = true,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.COMPLETED,
      recordedTime = LocalDateTime.now().minusDays(1),
    )
    assertThat(attendance.editable()).isFalse
  }
}
