package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

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
      .also { it.attendances.first().complete(attendanceReason(AttendanceReasonEnum.SUSPENDED)) }

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
      .also { it.attendances.first().complete(attendanceReason(AttendanceReasonEnum.AUTO_SUSPENDED)) }

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
      .hasMessage("${instance.activitySchedule.description} (${instance.timeSlot}) has already been cancelled")
  }

  @Test
  fun `cannot cancel a past scheduled instance`() {
    val date = today.minusWeeks(1)
    assertThatThrownBy {
      instance.copy(sessionDate = date).cancelSessionAndAttendances(
        reason = "Staff unavailable",
        by = "USER1",
        cancelComment = "Resume tomorrow",
        cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
      )
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("${instance.activitySchedule.description} (${instance.timeSlot}) has ended")
  }

  @Test
  fun `uncancelling scheduled instance ignores suspended attendances`() {
    val cancelledInstance = instance.copy(cancelled = true, cancelledReason = "cancelled reason", cancelledBy = "cancelled by", cancelledTime = LocalDateTime.now())
      .also { it.attendances.first().complete(attendanceReason(AttendanceReasonEnum.SUSPENDED)) }

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
      .also { it.attendances.first().complete(attendanceReason(AttendanceReasonEnum.AUTO_SUSPENDED)) }

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

  @Test
  fun `is future scheduled instance`() {
    val now = LocalTime.now()

    val scheduledInstance = instance.copy(sessionDate = today, startTime = now.plusMinutes(1), endTime = now.plusMinutes(30))
    assertThat(scheduledInstance.isFuture(LocalDateTime.now())).isTrue()

    val scheduledInstance2 = instance.copy(sessionDate = today, startTime = now.minusMinutes(1), endTime = now.plusMinutes(30))
    assertThat(scheduledInstance2.isFuture(LocalDateTime.now())).isFalse()
  }

  @Test
  fun `is future end scheduled instance`() {
    val now = LocalTime.now()

    val scheduledInstance = instance.copy(sessionDate = today, startTime = now.minusMinutes(30), endTime = now.plusMinutes(1))
    assertThat(scheduledInstance.isEndFuture(LocalDateTime.now())).isTrue()

    val scheduledInstance2 = instance.copy(sessionDate = today, startTime = now.minusMinutes(30), endTime = now.minusMinutes(1))
    assertThat(scheduledInstance2.isFuture(LocalDateTime.now())).isFalse()
  }

  @Nested
  inner class UpdateCancelledSessionAndAttendances {
    lateinit var cancelledInstance: ScheduledInstance

    @BeforeEach
    fun setUp() {
      cancelledInstance = instance.copy(
        scheduledInstanceId = 1,
        cancelled = true,
        cancelledBy = "Old user",
        cancelledReason = "Old reason",
        comment = "Old comment",
        cancelledTime = LocalDateTime.now().minusDays(1),
        attendances = mutableListOf(mock()),
      )
    }

    @Test
    fun `instance is updated when updating the reason without a comment`() {
      cancelledInstance.updateCancelledSessionAndAttendances("New reason", "New user", null)

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("New user")
        assertThat(cancelledReason).isEqualTo("New reason")
        assertThat(comment).isNull()
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))

        verify(attendances.first(), never()).updateCancelledAttendance(anyString(), anyString(), anyBoolean())
      }
    }

    @Test
    fun `instance is updated when updating the reason with with a comment`() {
      cancelledInstance.updateCancelledSessionAndAttendances("New reason", "New user", "New comment")

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("New user")
        assertThat(cancelledReason).isEqualTo("New reason")
        assertThat(comment).isEqualTo("New comment")
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))

        verify(attendances.first(), never()).updateCancelledAttendance(anyString(), anyString(), anyBoolean())
      }
    }

    @Test
    fun `instance is not updated when only updating issue payment to true`() {
      cancelledInstance.updateCancelledSessionAndAttendances(null, "New user", null, true)

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("Old user")
        assertThat(cancelledReason).isEqualTo("Old reason")
        assertThat(comment).isEqualTo("Old comment")
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now().minusDays(1), within(1, ChronoUnit.SECONDS))

        verify(attendances.first()).updateCancelledAttendance(null, "New user", true)
      }
    }

    @Test
    fun `instance is not updated when only updating issue payment to false`() {
      cancelledInstance.updateCancelledSessionAndAttendances(null, "New user", null, false)

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("Old user")
        assertThat(cancelledReason).isEqualTo("Old reason")
        assertThat(comment).isEqualTo("Old comment")
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now().minusDays(1), within(1, ChronoUnit.SECONDS))

        verify(attendances.first()).updateCancelledAttendance(null, "New user", false)
      }
    }

    @Test
    fun `instance is updated when updating reason, comment and issue payment`() {
      cancelledInstance.updateCancelledSessionAndAttendances("New reason", "New user", "New comment", false)

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("New user")
        assertThat(cancelledReason).isEqualTo("New reason")
        assertThat(comment).isEqualTo("New comment")
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))

        verify(attendances.first()).updateCancelledAttendance("New reason", "New user", false)
      }
    }

    @Test
    fun `instance is updated suspended attendances are not`() {
      val suspendedAttendance = mock<Attendance>()
      val unSuspendedAttendance = mock<Attendance>()

      whenever(suspendedAttendance.hasReason(AttendanceReasonEnum.SUSPENDED, AttendanceReasonEnum.AUTO_SUSPENDED)).thenReturn(true)

      cancelledInstance = cancelledInstance.copy(attendances = mutableListOf(suspendedAttendance, unSuspendedAttendance))

      cancelledInstance.updateCancelledSessionAndAttendances("New reason", "New user", "New comment", false)

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("New user")
        assertThat(cancelledReason).isEqualTo("New reason")
        assertThat(comment).isEqualTo("New comment")
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS))

        verify(suspendedAttendance, never()).updateCancelledAttendance(anyString(), anyString(), anyBoolean())
        verify(unSuspendedAttendance).updateCancelledAttendance("New reason", "New user", false)
      }
    }

    @Test
    fun `throws an exception when instance is not cancelled`() {
      assertThatThrownBy {
        instance.copy(cancelled = false).updateCancelledSessionAndAttendances("New reason", "New user", "New comment", false)
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot update ${instance.activitySchedule.description} (${instance.timeSlot}) because it is not cancelled")

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("Old user")
        assertThat(cancelledReason).isEqualTo("Old reason")
        assertThat(comment).isEqualTo("Old comment")
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now().minusDays(1), within(1, ChronoUnit.SECONDS))

        verifyNoInteractions(attendances.first())
      }
    }

    @Test
    fun `throws an exception when instance has ended`() {
      val date = today.minusDays(1)

      assertThatThrownBy {
        instance.copy(cancelled = true, sessionDate = date).updateCancelledSessionAndAttendances("New reason", "New user", "New comment", false)
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot update ${instance.activitySchedule.description} (${instance.timeSlot}) has ended")

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("Old user")
        assertThat(cancelledReason).isEqualTo("Old reason")
        assertThat(comment).isEqualTo("Old comment")
        assertThat(cancelledTime).isCloseTo(LocalDateTime.now().minusDays(1), within(1, ChronoUnit.SECONDS))

        verifyNoInteractions(attendances.first())
      }
    }
  }
}
