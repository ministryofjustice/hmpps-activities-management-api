package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import java.time.LocalDate

class ScheduledInstanceTest {

  private val instance = activityEntity().schedules.first().instances.first()

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
      instance.copy(cancelled = false, sessionDate = LocalDate.MIN).isRunningOn(LocalDate.MIN.plusDays(1))
    ).isFalse
    assertThat(
      instance.copy(cancelled = false, sessionDate = LocalDate.MAX).isRunningOn(LocalDate.MAX.minusDays(1))
    ).isFalse
  }

  @Test
  fun `instance session is not running on date when cancelled`() {
    assertThat(instance.copy(cancelled = true, sessionDate = LocalDate.MIN).isRunningOn(LocalDate.MIN)).isFalse
  }
}
