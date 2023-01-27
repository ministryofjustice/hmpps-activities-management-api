package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import java.time.LocalDate

class ActivityScheduleSuspensionTest {

  private val startDate = LocalDate.now()
  private val untilDate = startDate.plusDays(1)
  private val beforeStart = startDate.minusDays(1)
  private val afterUntil = untilDate.plusDays(1)
  private val schedule = activityEntity().schedules().first().apply { endDate = afterUntil }
  private val suspension = ActivityScheduleSuspension(
    activitySchedule = schedule,
    suspendedFrom = startDate,
    suspendedUntil = untilDate
  )

  @Test
  fun `check suspension is in effect`() {
    assertThat(suspension.isSuspendedOn(startDate)).isTrue
    assertThat(suspension.isSuspendedOn(untilDate)).isTrue
  }

  @Test
  fun `check suspension is not in effect`() {
    assertThat(suspension.isSuspendedOn(beforeStart)).isFalse
    assertThat(suspension.isSuspendedOn(afterUntil)).isFalse
  }

  @Test
  fun `fails if until date not after start`() {
    assertThatThrownBy {
      ActivityScheduleSuspension(
        activitySchedule = schedule,
        suspendedFrom = startDate,
        suspendedUntil = startDate
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Until date must be after suspend from date")
  }

  @Test
  fun `fails if slot dates outside of schedule dates`() {
    assertThatThrownBy {
      ActivityScheduleSuspension(
        activitySchedule = schedule,
        suspendedFrom = schedule.startDate.minusDays(1),
        suspendedUntil = untilDate
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Suspension dates must be the same or between the schedule dates")

    assertThatThrownBy {
      ActivityScheduleSuspension(
        activitySchedule = schedule,
        suspendedFrom = schedule.startDate,
        suspendedUntil = schedule.endDate!!.plusDays(1)
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Suspension dates must be the same or between the schedule dates")
  }
}
