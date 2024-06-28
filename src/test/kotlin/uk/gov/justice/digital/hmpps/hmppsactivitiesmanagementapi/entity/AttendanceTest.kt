package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class AttendanceTest {
  private val activity = activityEntity()
  private val activitySchedule = activity.schedules().first()
  private val instance = activitySchedule.instances().first()
  private val attendance = instance.attendances.first()

  @Test
  fun `uncancel method sets the state correctly`() {
    val attendance = Attendance(
      scheduledInstance = instance,
      prisonerNumber = "P000111",
      attendanceReason = attendanceReason(AttendanceReasonEnum.ATTENDED),
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
      assertThat(otherAbsenceReason).isNull()
    }
  }

  @Test
  fun `can cancel payable attendance independently of its instance`() {
    attendance.cancel(reason = attendanceReason(AttendanceReasonEnum.CANCELLED), cancelledReason = "By test reason", cancelledBy = "By test user")

    with(attendance) {
      assertThat(isPayable()).isTrue()
      assertThat(status()).isEqualTo(AttendanceStatus.COMPLETED)
      assertThat(issuePayment).isEqualTo(true)
      assertThat(attendanceReason).isEqualTo(attendanceReason(AttendanceReasonEnum.CANCELLED))
      assertThat(comment).isEqualTo("By test reason")
      assertThat(recordedTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
      assertThat(recordedBy).isEqualTo("By test user")
    }
  }

  @Test
  fun `can cancel unpayable attendance independently of its instance`() {
    activitySchedule(activityEntity(paid = false, noPayBands = true), paid = false).instances().first().attendances.first().let { unpayableAttendance ->
      unpayableAttendance.cancel(
        reason = attendanceReason(AttendanceReasonEnum.CANCELLED),
        cancelledReason = "By test reason",
        cancelledBy = "By test user",
      )

      with(unpayableAttendance) {
        isPayable() isBool false
        status() isEqualTo AttendanceStatus.COMPLETED
        issuePayment!! isBool false
        attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.CANCELLED)
        comment isEqualTo "By test reason"
        recordedTime isCloseTo TimeSource.now()
        recordedBy isEqualTo "By test user"
      }
    }
  }

  @Test
  fun `confirm cancel attendance defaults on cancel`() {
    with(attendance.cancel(reason = attendanceReason(AttendanceReasonEnum.CANCELLED))) {
      assertThat(recordedBy).isEqualTo("Activities Management Service")
      assertThat(comment).isNull()
    }
  }

  @Test
  fun `cancel attendance fails if already cancelled`() {
    val attendance = Attendance(
      scheduledInstance = instance,
      prisonerNumber = "123456",
      attendanceReason = attendanceReason(
        AttendanceReasonEnum.CANCELLED,
      ),
    )

    assertThatThrownBy {
      attendance.cancel(attendanceReason(AttendanceReasonEnum.CANCELLED))
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Attendance already cancelled")
  }

  @Test
  fun `cancel attendance fails if wrong reason code`() {
    assertThatThrownBy {
      Attendance(scheduledInstance = instance, prisonerNumber = "123456").cancel(attendanceReason(AttendanceReasonEnum.ATTENDED))
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Supplied reason code is not cancelled")
  }

  @Test
  fun `marking attendance records history`() {
    val otherAttendanceReason = attendanceReason(AttendanceReasonEnum.OTHER)
    val otherAttendance = attendance.copy(
      status = AttendanceStatus.COMPLETED,
      attendanceReason = otherAttendanceReason,
      otherAbsenceReason = "Other reason",
      comment = "Some comment",
      initialIssuePayment = true,
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
      assertThat(attendanceReason).isEqualTo(otherAttendanceReason)
      assertThat(comment).isEqualTo("Some comment")
      assertThat(recordedBy).isEqualTo("Joe Bloggs")
      assertThat(issuePayment).isEqualTo(true)
      assertThat(otherAbsenceReason).isEqualTo("Other reason")
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
  fun `attendance is editable - WAITING, session was 6 days ago`() {
    val instanceSixDaysAgo = instance.copy(sessionDate = LocalDate.now().minusDays(6))
    val attendance = Attendance(
      scheduledInstance = instanceSixDaysAgo,
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
      initialIssuePayment = true,
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
      initialIssuePayment = true,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.COMPLETED,
      recordedTime = LocalDateTime.now(),
      attendanceReason = attendanceReason(AttendanceReasonEnum.ATTENDED),
    )
    assertThat(attendance.editable()).isTrue
  }

  @Test
  fun `attendance is editable - COMPLETED, paid, session today, cancelled 5 days ago`() {
    val instanceToday = instance.copy(sessionDate = LocalDate.now(), startTime = LocalTime.of(1, 0), endTime = LocalTime.of(1, 30))
    val attendance = Attendance(
      scheduledInstance = instanceToday,
      initialIssuePayment = true,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.COMPLETED,
      recordedTime = LocalDateTime.now().minusDays(5),
      attendanceReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
    )
    assertThat(attendance.editable()).isTrue
  }

  @Test
  fun `attendance is editable - COMPLETED, unpaid, session was 6 days ago, marked six days ago, not attended`() {
    val instanceTenDaysAgo = instance.copy(sessionDate = LocalDate.now().minusDays(6))
    val attendance = Attendance(
      scheduledInstance = instanceTenDaysAgo,
      initialIssuePayment = false,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.COMPLETED,
      recordedTime = LocalDateTime.now().minusDays(6),
      attendanceReason = attendanceReason(AttendanceReasonEnum.SICK),
    )
    assertThat(attendance.editable()).isTrue
  }

  @Test
  fun `attendance is editable - COMPLETED, unpaid, session was today, attended`() {
    val instanceTenDaysAgo = instance.copy(sessionDate = LocalDate.now())
    val attendance = Attendance(
      scheduledInstance = instanceTenDaysAgo,
      initialIssuePayment = false,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.COMPLETED,
      recordedTime = LocalDateTime.now(),
      attendanceReason = attendanceReason(AttendanceReasonEnum.ATTENDED),
    )
    assertThat(attendance.editable()).isTrue
  }

  @Test
  fun `attendance is not editable - COMPLETED, paid, session was 1 day ago, marked 1 day ago, not attended`() {
    val instanceOneDaysAgo = instance.copy(sessionDate = LocalDate.now().minusDays(1))
    val attendance = Attendance(
      scheduledInstance = instanceOneDaysAgo,
      initialIssuePayment = true,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.COMPLETED,
      recordedTime = LocalDateTime.now().minusDays(1),
      attendanceReason = attendanceReason(AttendanceReasonEnum.SICK),
    )
    assertThat(attendance.editable()).isFalse()
  }

  @Test
  fun `attendance is NOT editable - COMPLETED, unpaid, session was 8 days ago, not attended`() {
    val instanceTenDaysAgo = instance.copy(sessionDate = LocalDate.now().minusDays(8))
    val attendance = Attendance(
      scheduledInstance = instanceTenDaysAgo,
      initialIssuePayment = false,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.COMPLETED,
      recordedTime = LocalDateTime.now().minusDays(8),
      attendanceReason = attendanceReason(AttendanceReasonEnum.SICK),
    )
    assertThat(attendance.editable()).isFalse()
  }

  @Test
  fun `attendance is NOT editable - COMPLETED, unpaid, session was 6 day ago, recorded 2 days ago, attended`() {
    val instanceSixDaysAgo = instance.copy(sessionDate = LocalDate.now().minusDays(6))
    val attendance = Attendance(
      scheduledInstance = instanceSixDaysAgo,
      initialIssuePayment = false,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.COMPLETED,
      recordedTime = LocalDateTime.now().minusDays(2),
      attendanceReason = attendanceReason(AttendanceReasonEnum.ATTENDED),
    )
    assertThat(attendance.editable()).isFalse()
  }

  @Test
  fun `attendance is NOT editable - WAITING, session was 7 days ago`() {
    val instanceSevenDaysAgo = instance.copy(sessionDate = LocalDate.now().minusDays(7))
    val attendance = Attendance(
      scheduledInstance = instanceSevenDaysAgo,
      initialIssuePayment = false,
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
      initialIssuePayment = true,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.COMPLETED,
      recordedTime = LocalDateTime.now().minusDays(1),
    )
    assertThat(attendance.editable()).isFalse
  }

  @Test
  fun `marking attendance should fail if record isn't editable`() {
    val expiredInstance = instance.copy(sessionDate = LocalDate.now().minusDays(15))
    val attendance = Attendance(
      scheduledInstance = expiredInstance,
      initialIssuePayment = false,
      prisonerNumber = "A1234AA",
      status = AttendanceStatus.WAITING,
    )

    assertThatThrownBy {
      attendance.mark(
        principalName = "New user",
        reason = attendanceReason(AttendanceReasonEnum.ATTENDED),
        newStatus = AttendanceStatus.COMPLETED,
        newComment = null,
        newIssuePayment = null,
        newIncentiveLevelWarningIssued = null,
        newCaseNoteId = null,
        newOtherAbsenceReason = null,
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Attendance record for prisoner 'A1234AA' can no longer be modified")
  }

  @Test
  fun `can complete attendance without payment`() {
    val attendance = with(Attendance(scheduledInstance = instance, prisonerNumber = "123456")) {
      assertThat(status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(issuePayment).isNull()
      assertThat(recordedTime).isNull()
      assertThat(recordedBy).isNull()
      assertThat(attendanceReason).isNull()
      assertThat(history()).isEmpty()
      this
    }

    val reason = mock<AttendanceReason>()

    with(attendance.completeWithoutPayment(reason)) {
      assertThat(status()).isEqualTo(AttendanceStatus.COMPLETED)
      assertThat(issuePayment).isFalse()
      assertThat(recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(recordedBy).isEqualTo("Activities Management Service")
      assertThat(attendanceReason).isEqualTo(reason)
      assertThat(history()).hasSize(1)
    }
  }

  @Test
  fun `cannot complete attendance without payment when no longer editable`() {
    val attendance = Attendance(
      scheduledInstance = instance.copy(sessionDate = LocalDate.now().minusWeeks(3)),
      prisonerNumber = "123456",
      status = AttendanceStatus.COMPLETED,
    ).also { assertThat(it.editable()).isFalse() }

    assertThatThrownBy {
      attendance.completeWithoutPayment(mock())
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Attendance record for prisoner '123456' can no longer be modified")
  }

  @Test
  fun `resetting attendance record`() {
    val attendance = Attendance(
      scheduledInstance = instance,
      prisonerNumber = "P000111",
      attendanceReason = attendanceReason(AttendanceReasonEnum.ATTENDED),
      status = AttendanceStatus.COMPLETED,
      comment = "Some Comment",
      recordedTime = LocalDateTime.now(),
    )

    attendance.resetAttendance("reset by test")

    with(attendance) {
      assertThat(attendanceReason).isNull()
      assertThat(status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(comment).isNull()
      assertThat(otherAbsenceReason).isNull()
      assertThat(recordedTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))
      assertThat(recordedBy).isEqualTo("reset by test")
    }
  }

  @Test
  fun `attempting to reset a waiting attendance record fails`() {
    val attendance = Attendance(
      scheduledInstance = instance,
      prisonerNumber = "P000111",
      status = AttendanceStatus.WAITING,
    )

    assertThatThrownBy {
      attendance.resetAttendance("reset by test")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot reset an attendance that is already WAITING")
  }

  @Test
  fun `can unsuspend an auto-suspended attendance to waiting and history records are created`() {
    Attendance(scheduledInstance = instance, prisonerNumber = "123456")
      .also { assertThat(it.history()).isEmpty() }
      .completeWithoutPayment(attendanceReason(AttendanceReasonEnum.AUTO_SUSPENDED))
      .also { assertThat(it.history()).hasSize(1) }
      .unsuspend()
      .also {
        assertThat(it.history()).hasSize(2)
        assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
        assertThat(it.attendanceReason).isNull()
        assertThat(it.issuePayment).isNull()
        assertThat(it.recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
        assertThat(it.recordedBy).isEqualTo("Activities Management Service")
      }
  }

  @Test
  fun `can unsuspend a suspended attendance to waiting and history records are created`() {
    Attendance(scheduledInstance = instance, prisonerNumber = "123456")
      .also { assertThat(it.history()).isEmpty() }
      .completeWithoutPayment(attendanceReason(AttendanceReasonEnum.SUSPENDED))
      .also { assertThat(it.history()).hasSize(1) }
      .unsuspend()
      .also {
        assertThat(it.history()).hasSize(2)
        assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
        assertThat(it.attendanceReason).isNull()
        assertThat(it.issuePayment).isNull()
        assertThat(it.recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
        assertThat(it.recordedBy).isEqualTo("Activities Management Service")
      }
  }

  @Test
  fun `reset waiting attendance fails if the attendance is not suspended`() {
    val attendance = Attendance(scheduledInstance = instance, prisonerNumber = "123456").also {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
    }

    assertThatThrownBy {
      attendance.unsuspend()
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Attendance must be suspended to unsuspend it")
  }
}
