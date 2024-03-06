package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduledInstanceTest {

  private val instance = activityEntity().schedules().first().instances().first()
  private val today: LocalDate = TimeSource.today()

  @Test
  fun `instance is not cancelled`() {
    assertThat(instance.copy(cancelled = false).cancelled).isFalse
  }

  @Test
  fun `instance is cancelled`() {
    assertThat(instance.copy(cancelled = true).cancelled).isTrue
  }

  @Test
  fun `instance session is running on date when not cancelled`() {
    assertThat(instance.copy(cancelled = false, sessionDate = LocalDate.MIN).isRunningOn(LocalDate.MIN)).isTrue
  }

  @Test
  fun `instance session is not running on dates`() {
    assertThat(
      instance.copy(cancelled = false, sessionDate = LocalDate.MIN).isRunningOn(LocalDate.MIN.plusDays(1)),
    ).isFalse
    assertThat(
      instance.copy(cancelled = false, sessionDate = LocalDate.MAX).isRunningOn(LocalDate.MAX.minusDays(1)),
    ).isFalse
  }

  @Test
  fun `instance session is not running on date when cancelled`() {
    assertThat(instance.copy(cancelled = true, sessionDate = LocalDate.MIN).isRunningOn(LocalDate.MIN)).isFalse
  }

  @Test
  fun `instance state is set correctly when uncancelled`() {
    val cancelledInstance = instance.copy(
      scheduledInstanceId = 1,
      cancelled = true,
      cancelledBy = "DEF981",
      cancelledReason = "Meeting Cancelled",
      attendances = mutableListOf(mock()),
    )

    cancelledInstance.uncancelSessionAndAttendances()

    with(cancelledInstance) {
      assertThat(cancelled).isFalse
      assertThat(cancelledBy).isNull()
      assertThat(cancelledReason).isNull()

      verify(attendances.first()).uncancel()
    }
  }

  @Test
  fun `instance cannot be uncancelled if the session date is in the past`() {
    val exception = assertThrows<IllegalArgumentException> {
      instance.copy(
        scheduledInstanceId = 1,
        cancelled = true,
        cancelledBy = "DEF981",
        cancelledReason = "Meeting Cancelled",
        sessionDate = LocalDate.of(2022, 1, 1),
      ).uncancelSessionAndAttendances()
    }

    assertThat(exception.message).isEqualTo("Cannot uncancel scheduled instance [1] because it is in the past")
  }

  @Test
  fun `instance cannot be uncancelled if it is not already cancelled`() {
    val exception = assertThrows<IllegalArgumentException> {
      instance.copy(
        scheduledInstanceId = 1,
      ).uncancelSessionAndAttendances()
    }

    assertThat(exception.message).isEqualTo("Cannot uncancel scheduled instance [1] because it is not cancelled")
  }

  @Test
  fun `can cancel scheduled instance`() {
    val cancelableInstance = instance.copy()
    cancelableInstance.cancelSessionAndAttendances(
      reason = "Staff unavailable",
      by = "USER1",
      cancelComment = "Resume tomorrow",
      cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
    )

    with(cancelableInstance) {
      assertThat(cancelledReason).isEqualTo("Staff unavailable")
      assertThat(cancelled).isTrue
      assertThat(cancelledBy).isEqualTo("USER1")
      assertThat(cancelledTime).isNotNull
      assertThat(comment).isEqualTo("Resume tomorrow")

      attendances.forEach { it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.CANCELLED) }
    }
  }

  @Test
  fun `cancelling scheduled instance ignores suspended attendances`() {
    val cancelableInstance = instance.copy()
      .also { it.attendances.first().completeWithoutPayment(attendanceReason(AttendanceReasonEnum.SUSPENDED)) }

    cancelableInstance.cancelSessionAndAttendances(
      reason = "Staff unavailable",
      by = "USER1",
      cancelComment = "Resume tomorrow",
      cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
    )

    with(cancelableInstance) {
      assertThat(cancelledReason).isEqualTo("Staff unavailable")
      assertThat(cancelled).isTrue
      assertThat(cancelledBy).isEqualTo("USER1")
      assertThat(cancelledTime).isNotNull
      assertThat(comment).isEqualTo("Resume tomorrow")

      attendances.forEach { it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.SUSPENDED) }
    }
  }

  @Test
  fun `cancelling scheduled instance ignores auto-suspended attendances`() {
    val cancelableInstance = instance.copy()
      .also { it.attendances.first().completeWithoutPayment(attendanceReason(AttendanceReasonEnum.AUTO_SUSPENDED)) }

    cancelableInstance.cancelSessionAndAttendances(
      reason = "Staff unavailable",
      by = "USER1",
      cancelComment = "Resume tomorrow",
      cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
    )

    with(cancelableInstance) {
      assertThat(cancelledReason).isEqualTo("Staff unavailable")
      assertThat(cancelled).isTrue
      assertThat(cancelledBy).isEqualTo("USER1")
      assertThat(cancelledTime).isNotNull
      assertThat(comment).isEqualTo("Resume tomorrow")

      attendances.single { it.attendanceReason == attendanceReason(AttendanceReasonEnum.AUTO_SUSPENDED) }
    }
  }

  @Test
  fun `cannot cancel scheduled instance that's already cancelled`() {
    assertThatThrownBy {
      instance.copy(cancelled = true).cancelSessionAndAttendances(
        reason = "Staff unavailable",
        by = "USER1",
        cancelComment = "Resume tomorrow",
        cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The schedule instance has already been cancelled")
  }

  @Test
  fun `cannot cancel a past scheduled instance`() {
    assertThatThrownBy {
      instance.copy(sessionDate = today.minusWeeks(1)).cancelSessionAndAttendances(
        reason = "Staff unavailable",
        by = "USER1",
        cancelComment = "Resume tomorrow",
        cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The schedule instance has ended")
  }

  @Test
  fun `uncancelling scheduled instance ignores suspended attendances`() {
    val cancelledInstance = instance.copy(cancelled = true, cancelledReason = "cancelled reason", cancelledBy = "cancelled by", cancelledTime = LocalDateTime.now())
      .also { it.attendances.first().completeWithoutPayment(attendanceReason(AttendanceReasonEnum.SUSPENDED)) }

    cancelledInstance.uncancelSessionAndAttendances()

    with(cancelledInstance) {
      assertThat(cancelledReason).isNull()
      assertThat(cancelled).isFalse
      assertThat(cancelledBy).isNull()
      assertThat(cancelledTime).isNull()

      attendances.forEach { it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.SUSPENDED) }
    }
  }

  @Test
  fun `uncancelling scheduled instance ignores auto-suspended attendances`() {
    val cancelledInstance = instance.copy(cancelled = true, cancelledReason = "cancelled reason", cancelledBy = "cancelled by", cancelledTime = LocalDateTime.now())
      .also { it.attendances.first().completeWithoutPayment(attendanceReason(AttendanceReasonEnum.AUTO_SUSPENDED)) }

    cancelledInstance.uncancelSessionAndAttendances()

    with(cancelledInstance) {
      assertThat(cancelledReason).isNull()
      assertThat(cancelled).isFalse
      assertThat(cancelledBy).isNull()
      assertThat(cancelledTime).isNull()

      attendances.single { it.attendanceReason == attendanceReason(AttendanceReasonEnum.AUTO_SUSPENDED) }
    }
  }

  @Test
  fun `can remove attendance`() {
    val attendance = instance.attendances.first()

    assertThat(instance.attendances).contains(attendance)

    instance.remove(attendance)

    assertThat(instance.attendances).doesNotContain(attendance)
  }

  @Test
  fun `fails to remove attendance if attendance not present`() {
    val attendance = instance.attendances.first()

    instance.remove(attendance)

    assertThatThrownBy { instance.remove(attendance) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Attendance record with ${attendance.attendanceId} does not exist on the scheduled instance")
  }

  @Test
  fun `session is paid`() {
    val paidSession = activityEntity(paid = true).schedules().first()

    paidSession.isPaid() isBool true
  }

  @Test
  fun `session is not paid`() {
    val unpaidSession = activityEntity(paid = false, noPayBands = true).schedules().first()

    unpaidSession.isPaid() isBool false
  }
}
