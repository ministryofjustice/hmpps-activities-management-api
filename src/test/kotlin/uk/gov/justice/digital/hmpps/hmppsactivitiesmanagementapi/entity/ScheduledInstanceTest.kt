package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import java.time.LocalDate

class ScheduledInstanceTest {

  private val instance = activityEntity().schedules().first().instances().first()

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
}
