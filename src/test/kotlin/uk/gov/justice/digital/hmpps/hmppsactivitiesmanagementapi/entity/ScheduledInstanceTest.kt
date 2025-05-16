package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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

  @Nested
  inner class UncancelSessionAndAttendances {
    lateinit var cancelledInstance: ScheduledInstance

    @BeforeEach
    fun setUp() {
      cancelledInstance = activityEntity().schedules().first().instances().first()
        .also {
          it.cancelSessionAndAttendances(
            reason = "Old reason",
            by = "Old user",
            cancelComment = "Old comment",
            cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
            false,
            true,
          )
        }
    }

    @Test
    @Deprecated("Remove when toggle FEATURE_CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED is removed")
    fun `instance state is set correctly when uncancelled - old`() {
      cancelledInstance.uncancelSessionAndAttendances(false)

      with(cancelledInstance) {
        assertThat(cancelled).isFalse
        assertThat(cancelledBy).isNull()
        assertThat(cancelledReason).isNull()
        assertThat(cancelledIssuePayment).isNull()

        attendances.forEach {
          it.attendanceReason isEqualTo null
          it.comment isEqualTo null
          it.issuePayment isEqualTo null
        }
      }
    }

    @Test
    fun `instance state is set correctly when uncancelled`() {
      cancelledInstance.uncancelSessionAndAttendances(true)

      with(cancelledInstance) {
        assertThat(cancelled).isFalse
        assertThat(cancelledBy).isNull()
        assertThat(cancelledReason).isNull()
        assertThat(cancelledIssuePayment).isNull()

        attendances.forEach {
          it.attendanceReason isEqualTo null
          it.comment isEqualTo null
          it.issuePayment isEqualTo null
        }
      }
    }

    @Test
    @Deprecated("Remove when toggle FEATURE_CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED is removed")
    fun `instance cannot be uncancelled if the session date is in the past - old`() {
      assertThatThrownBy {
        cancelledInstance.copy(sessionDate = LocalDate.now().minusDays(1)).uncancelSessionAndAttendances(false)
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot uncancel scheduled instance [0] because it is in the past")

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("Old user")
        assertThat(cancelledReason).isEqualTo("Old reason")
        assertThat(comment).isEqualTo("Old comment")
        assertThat(cancelledTime).isNotNull()
        assertThat(cancelledIssuePayment).isFalse()

        attendances.forEach {
          it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.CANCELLED)
          it.comment isEqualTo "Old reason"
          it.issuePayment isEqualTo false
        }
      }
    }

    @Test
    fun `instance cannot be uncancelled if the session date is in the past`() {
      assertThatThrownBy {
        cancelledInstance.copy(sessionDate = LocalDate.now().minusDays(1)).uncancelSessionAndAttendances(true)
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot uncancel scheduled instance [0] because it is in the past")

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("Old user")
        assertThat(cancelledReason).isEqualTo("Old reason")
        assertThat(comment).isEqualTo("Old comment")
        assertThat(cancelledTime).isNotNull()
        assertThat(cancelledIssuePayment).isFalse()

        attendances.forEach {
          it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.CANCELLED)
          it.comment isEqualTo "Old reason"
          it.issuePayment isEqualTo false
        }
      }
    }

    @Test
    @Deprecated("Remove when toggle FEATURE_CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED is removed")
    fun `instance cannot be uncancelled if it is not already cancelled - old`() {
      assertThatThrownBy {
        instance.copy(scheduledInstanceId = 1).uncancelSessionAndAttendances(false)
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot uncancel scheduled instance [1] because it is not cancelled")

      with(instance) {
        assertThat(cancelled).isFalse
        assertThat(cancelledBy).isNull()
        assertThat(cancelledReason).isNull()
        assertThat(comment).isNull()
        assertThat(cancelledTime).isNull()
        assertThat(cancelledIssuePayment).isNull()

        attendances.forEach {
          it.attendanceReason isEqualTo null
          it.comment isEqualTo null
          it.issuePayment isEqualTo null
        }
      }
    }

    @Test
    fun `instance cannot be uncancelled if it is not already cancelled`() {
      assertThatThrownBy {
        instance.copy(scheduledInstanceId = 1).uncancelSessionAndAttendances(true)
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot uncancel scheduled instance [1] because it is not cancelled")

      with(instance) {
        assertThat(cancelled).isFalse
        assertThat(cancelledBy).isNull()
        assertThat(cancelledReason).isNull()
        assertThat(comment).isNull()
        assertThat(cancelledTime).isNull()
        assertThat(cancelledIssuePayment).isNull()

        attendances.forEach {
          it.attendanceReason isEqualTo null
          it.comment isEqualTo null
          it.issuePayment isEqualTo null
        }
      }
    }

    @EnumSource(AttendanceReasonEnum::class, names = ["CANCELLED"], mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest(name = "instance will be cancelled but attendance will not change where reason is {0}")
    fun `instance will be cancelled but attendance will not change where reason is`(reason: AttendanceReasonEnum) {
      cancelledInstance.attendances.first().mark(
        principalName = "Old user",
        reason = attendanceReason(reason),
        newStatus = AttendanceStatus.COMPLETED,
        newComment = "Old reason",
        newIssuePayment = true,
        newIncentiveLevelWarningIssued = null,
        newCaseNoteId = null,
        newOtherAbsenceReason = null,
      )

      cancelledInstance.uncancelSessionAndAttendances(true)

      with(cancelledInstance) {
        assertThat(cancelled).isFalse
        assertThat(cancelledBy).isNull()
        assertThat(cancelledReason).isNull()
        assertThat(cancelledIssuePayment).isNull()

        attendances.forEach {
          it.attendanceReason isEqualTo attendanceReason(reason)
          it.comment isEqualTo "Old reason"
          it.issuePayment isEqualTo true
        }
      }
    }
  }

  @Test
  @Deprecated("Remove when toggle FEATURE_CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED is removed")
  fun `can cancel scheduled instance - old`() {
    val cancellableInstance = instance.copy()

    cancellableInstance.cancelSessionAndAttendances(
      reason = "Staff unavailable",
      by = "USER1",
      cancelComment = "Resume tomorrow",
      cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
      false,
    )

    with(cancellableInstance) {
      assertThat(cancelledReason).isEqualTo("Staff unavailable")
      assertThat(cancelled).isTrue
      assertThat(cancelledBy).isEqualTo("USER1")
      assertThat(cancelledTime).isNotNull
      assertThat(cancelledIssuePayment).isFalse()
      assertThat(comment).isEqualTo("Resume tomorrow")

      attendances.forEach {
        it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.CANCELLED)
        it.comment isEqualTo "Staff unavailable"
        it.issuePayment isEqualTo false
      }
    }
  }

  @Test
  fun `can cancel scheduled instance with not recorded attendance`() {
    val cancellableInstance = instance.copy()

    cancellableInstance.cancelSessionAndAttendances(
      reason = "Staff unavailable",
      by = "USER1",
      cancelComment = "Resume tomorrow",
      cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
      useNewPriorityRules = true,
    )

    with(cancellableInstance) {
      assertThat(cancelledReason).isEqualTo("Staff unavailable")
      assertThat(cancelled).isTrue
      assertThat(cancelledBy).isEqualTo("USER1")
      assertThat(cancelledTime).isNotNull
      assertThat(cancelledIssuePayment).isNull()
      assertThat(comment).isEqualTo("Resume tomorrow")

      attendances.forEach {
        it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.CANCELLED)
        it.comment isEqualTo "Staff unavailable"
        it.issuePayment isEqualTo true
      }
    }
  }

  @Test
  @Deprecated("Remove when toggle FEATURE_CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED is removed")
  fun `cancelling scheduled instance ignores suspended attendances - old`() {
    val cancellableInstance = instance.copy()
      .also { it.attendances.first().complete(attendanceReason(AttendanceReasonEnum.SUSPENDED)) }

    cancellableInstance.cancelSessionAndAttendances(
      reason = "Staff unavailable",
      by = "USER1",
      cancelComment = "Resume tomorrow",
      cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
      issuePayment = true,
    )

    with(cancellableInstance) {
      assertThat(cancelledReason).isEqualTo("Staff unavailable")
      assertThat(cancelled).isTrue
      assertThat(cancelledBy).isEqualTo("USER1")
      assertThat(cancelledTime).isNotNull
      assertThat(cancelledIssuePayment).isTrue
      assertThat(comment).isEqualTo("Resume tomorrow")

      attendances.forEach {
        it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.SUSPENDED)
        it.comment isEqualTo null
        it.issuePayment isEqualTo false
      }
    }
  }

  @EnumSource(AttendanceReasonEnum::class, names = ["ATTENDED"], mode = EnumSource.Mode.EXCLUDE)
  @ParameterizedTest(name = "cancelling scheduled instance ignores attendance where reason is {0}")
  fun `cancelling scheduled instance ignores attendance where reason is`(attendanceReason: AttendanceReasonEnum) {
    val cancellableInstance = instance.copy()
      .also { it.attendances.first().complete(attendanceReason(attendanceReason)) }

    cancellableInstance.cancelSessionAndAttendances(
      reason = "Staff unavailable",
      by = "USER1",
      cancelComment = "Resume tomorrow",
      cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
      issuePayment = true,
      useNewPriorityRules = true,
    )

    with(cancellableInstance) {
      assertThat(cancelledReason).isEqualTo("Staff unavailable")
      assertThat(cancelled).isTrue
      assertThat(cancelledBy).isEqualTo("USER1")
      assertThat(cancelledTime).isNotNull
      assertThat(cancelledIssuePayment).isTrue
      assertThat(comment).isEqualTo("Resume tomorrow")

      attendances.forEach {
        it.attendanceReason isEqualTo attendanceReason(attendanceReason)
        it.comment isEqualTo null
        it.issuePayment isEqualTo false
      }
    }
  }

  @Test
  @Deprecated("Remove when toggle FEATURE_CANCEL_INSTANCE_PRIORITY_CHANGE_ENABLED is removed")
  fun `cancelling scheduled instance ignores auto-suspended attendances - old`() {
    val cancellableInstance = instance.copy()
      .also { it.attendances.first().complete(attendanceReason(AttendanceReasonEnum.AUTO_SUSPENDED)) }

    cancellableInstance.cancelSessionAndAttendances(
      reason = "Staff unavailable",
      by = "USER1",
      cancelComment = "Resume tomorrow",
      cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
      issuePayment = false,
    )

    with(cancellableInstance) {
      assertThat(cancelledReason).isEqualTo("Staff unavailable")
      assertThat(cancelled).isTrue
      assertThat(cancelledBy).isEqualTo("USER1")
      assertThat(cancelledTime).isNotNull
      assertThat(cancelledIssuePayment).isFalse
      assertThat(comment).isEqualTo("Resume tomorrow")

      attendances.forEach {
        it.attendanceReason == attendanceReason(AttendanceReasonEnum.AUTO_SUSPENDED)
        it.comment isEqualTo null
        it.issuePayment isEqualTo false
      }
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
    val cancelledInstance = instance.copy(
      cancelled = true,
      cancelledReason = "cancelled reason",
      cancelledBy = "cancelled by",
      cancelledTime = LocalDateTime.now(),
      cancelledIssuePayment = false,
    )
      .also { it.attendances.first().complete(attendanceReason(AttendanceReasonEnum.SUSPENDED)) }

    cancelledInstance.uncancelSessionAndAttendances()

    with(cancelledInstance) {
      assertThat(cancelledReason).isNull()
      assertThat(cancelled).isFalse
      assertThat(cancelledBy).isNull()
      assertThat(cancelledTime).isNull()
      assertThat(cancelledIssuePayment).isNull()

      attendances.forEach { it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.SUSPENDED) }
    }
  }

  @Test
  fun `uncancelling scheduled instance ignores auto-suspended attendances`() {
    val cancelledInstance = instance.copy(
      cancelled = true,
      cancelledReason = "cancelled reason",
      cancelledBy = "cancelled by",
      cancelledTime = LocalDateTime.now(),
      cancelledIssuePayment = true,
    )
      .also { it.attendances.first().complete(attendanceReason(AttendanceReasonEnum.AUTO_SUSPENDED)) }

    cancelledInstance.uncancelSessionAndAttendances()

    with(cancelledInstance) {
      assertThat(cancelledReason).isNull()
      assertThat(cancelled).isFalse
      assertThat(cancelledBy).isNull()
      assertThat(cancelledTime).isNull()
      assertThat(cancelledIssuePayment).isNull()

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
      cancelledInstance = activityEntity().schedules().first().instances().first()
        .also {
          it.cancelSessionAndAttendances(
            reason = "Old reason",
            by = "Old user",
            cancelComment = "Old comment",
            cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
            false,
            true,
          )
        }
    }

    @Test
    fun `instance is updated but attendance is not when updating the reason without a comment and without changing issue payment`() {
      cancelledInstance.updateCancelledSessionAndAttendances("New reason", "New user", null)

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("New user")
        assertThat(cancelledReason).isEqualTo("New reason")
        assertThat(comment).isNull()
        assertThat(cancelledIssuePayment).isFalse

        attendances.forEach {
          it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.CANCELLED)
          it.comment isEqualTo "Old reason"
          it.issuePayment isEqualTo false
        }
      }
    }

    @Test
    fun `instance is updated but attendance is not when updating the reason with with a comment and without changing issue payment`() {
      cancelledInstance.updateCancelledSessionAndAttendances("New reason", "New user", "New comment")

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("New user")
        assertThat(cancelledReason).isEqualTo("New reason")
        assertThat(comment).isEqualTo("New comment")
        assertThat(cancelledIssuePayment).isFalse

        attendances.forEach {
          it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.CANCELLED)
          it.comment isEqualTo "Old reason"
          it.issuePayment isEqualTo false
        }
      }
    }

    @Test
    fun `instance is not updated but attendance is when only updating issue payment to true`() {
      cancelledInstance.updateCancelledSessionAndAttendances(null, "New user", null, true)

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("Old user")
        assertThat(cancelledReason).isEqualTo("Old reason")
        assertThat(comment).isEqualTo("Old comment")
        assertThat(cancelledIssuePayment).isTrue

        attendances.forEach {
          it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.CANCELLED)
          it.comment isEqualTo "Old reason"
          it.issuePayment isEqualTo true
        }
      }
    }

    @Test
    fun `instance is not updated when only updating issue payment to false`() {
      val cancelledInstance = cancelledInstance.copy(cancelledIssuePayment = true)

      cancelledInstance.updateCancelledSessionAndAttendances(null, "New user", null, false)

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("Old user")
        assertThat(cancelledReason).isEqualTo("Old reason")
        assertThat(comment).isEqualTo("Old comment")
        assertThat(cancelledIssuePayment).isFalse

        attendances.forEach {
          it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.CANCELLED)
          it.comment isEqualTo "Old reason"
          it.issuePayment isEqualTo false
        }
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
        assertThat(cancelledIssuePayment).isFalse

        attendances.forEach {
          it.attendanceReason isEqualTo attendanceReason(AttendanceReasonEnum.CANCELLED)
          it.comment isEqualTo "New reason"
          it.issuePayment isEqualTo false
        }
      }
    }

    @EnumSource(AttendanceReasonEnum::class, names = ["CANCELLED", "ATTENDED"], mode = EnumSource.Mode.EXCLUDE)
    @ParameterizedTest(name = "instance is updated but non-cancelled attendance is not where reason is {0}")
    fun `instance is updated but non-cancelled attendance is not`(reason: AttendanceReasonEnum) {
      instance
        .also {
          it.attendances.first().mark(
            principalName = "Old user",
            reason = attendanceReason(reason),
            newStatus = AttendanceStatus.COMPLETED,
            newComment = "Old reason",
            newIssuePayment = true,
            newIncentiveLevelWarningIssued = null,
            newCaseNoteId = null,
            newOtherAbsenceReason = null,
          )
        }.also {
          it.cancelSessionAndAttendances(
            reason = "Staff unavailable",
            by = "USER1",
            cancelComment = "Resume tomorrow",
            cancellationReason = attendanceReason(AttendanceReasonEnum.CANCELLED),
            false,
            true,
          )
        }

      instance.updateCancelledSessionAndAttendances("New reason", "New user", "New comment", false)

      with(instance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("New user")
        assertThat(cancelledReason).isEqualTo("New reason")
        assertThat(comment).isEqualTo("New comment")
        assertThat(cancelledIssuePayment).isFalse

        attendances.forEach {
          it.attendanceReason isEqualTo attendanceReason(reason)
          it.comment isEqualTo "Old reason"
          it.issuePayment isEqualTo true
        }
      }
    }

    @Test
    fun `throws an exception when instance is not cancelled`() {
      assertThatThrownBy {
        instance.updateCancelledSessionAndAttendances("New reason", "New user", "New comment", false)
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot update ${instance.activitySchedule.description} (${instance.timeSlot}) because it is not cancelled")

      with(instance) {
        assertThat(cancelled).isFalse
        assertThat(cancelledBy).isNull()
        assertThat(cancelledReason).isNull()
        assertThat(comment).isNull()
        assertThat(cancelledIssuePayment).isNull()

        attendances.forEach {
          it.comment isEqualTo null
          it.issuePayment isEqualTo null
        }
      }
    }

    @Test
    fun `throws an exception when instance has ended`() {
      val date = today.minusDays(1)

      assertThatThrownBy {
        cancelledInstance.copy(sessionDate = date).updateCancelledSessionAndAttendances("New reason", "New user", "New comment", false)
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot update ${instance.activitySchedule.description} (${instance.timeSlot}) has ended")

      with(cancelledInstance) {
        assertThat(cancelled).isTrue
        assertThat(cancelledBy).isEqualTo("Old user")
        assertThat(cancelledReason).isEqualTo("Old reason")
        assertThat(comment).isEqualTo("Old comment")
        assertThat(cancelledIssuePayment).isFalse

        attendances.forEach {
          it.comment isEqualTo "Old reason"
          it.issuePayment isEqualTo false
        }
      }
    }
  }
}
