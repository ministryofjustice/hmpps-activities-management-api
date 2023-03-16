package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.attendanceReasons
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduledInstanceTest {

  private val instance = activityEntity().schedules().first().instances().first()
  val today: LocalDateTime = LocalDateTime.now()

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

    cancelledInstance.uncancel()

    with(cancelledInstance) {
      assertThat(cancelled).isFalse
      assertThat(cancelledBy).isNull()
      assertThat(cancelledReason).isNull()

      verify(attendances.first()).waiting()
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
      ).uncancel()
    }

    assertThat(exception.message).isEqualTo("Cannot uncancel scheduled instance [1] because it is in the past")
  }

  @Test
  fun `instance cannot be uncancelled if it is not already cancelled`() {
    val exception = assertThrows<IllegalArgumentException> {
      instance.copy(
        scheduledInstanceId = 1,
      ).uncancel()
    }

    assertThat(exception.message).isEqualTo("Cannot uncancel scheduled instance [1] because it is not cancelled")
  }

  @Test
  fun `can cancel scheduled instance`() {
    val cancelableInstance = instance.copy()
    cancelableInstance.cancelSession(
      reason = "Staff unavailable",
      by = "USER1",
      cancelComment = "Resume tomorrow",
    ) {
      it.forEach { attendance -> attendance.cancel(attendanceReasons()["CANC"]!!, "BAS") }
    }
    with(cancelableInstance) {
      assertThat(cancelledReason).isEqualTo("Staff unavailable")
      assertThat(cancelled).isTrue
      assertThat(cancelledBy).isEqualTo("USER1")
      assertThat(cancelledTime).isNotNull
      assertThat(comment).isEqualTo("Resume tomorrow")

      with(attendances.first()) {
        assertThat(attendanceReason).isEqualTo(attendanceReason)
      }
    }
  }

  @Test
  fun `cannot cancel scheduled instance that's already cancelled`() {
    assertThatThrownBy {
      instance.copy(cancelled = true).cancelSession(
        reason = "Staff unavailable",
        by = "USER1",
        cancelComment = "Resume tomorrow",
      ) {
        it.forEach { attendance -> attendance.cancel(attendanceReasons()["CANC"]!!, "BAS") }
      }
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The schedule instance has already been cancelled")
  }

  @Test
  fun `cannot cancel a past scheduled instance`() {
    assertThatThrownBy {
      instance.copy(sessionDate = today.minusDays(7).toLocalDate()).cancelSession(
        reason = "Staff unavailable",
        by = "USER1",
        cancelComment = "Resume tomorrow",
      ) {
        it.forEach { attendance -> attendance.cancel(attendanceReasons()["CANC"]!!, "BAS") }
      }
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The schedule instance has ended")
  }
}
