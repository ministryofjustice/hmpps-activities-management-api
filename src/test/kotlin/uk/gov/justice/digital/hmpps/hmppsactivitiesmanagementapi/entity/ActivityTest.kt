package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import java.time.LocalDate

class ActivityTest {

  @Test
  fun `activity is inactive`() {
    assertThat(activityEntity().copy(active = false).active).isFalse
  }

  @Test
  fun `activity is active`() {
    assertThat(activityEntity().copy(active = true).active).isTrue
  }

  @Test
  fun `activity is active on open ended date range`() {
    assertThat(activityEntity().copy(active = true, startDate = LocalDate.MIN, endDate = null).isActiveOn(LocalDate.MAX)).isTrue
  }

  @Test
  fun `activity is active on closed date range when in range`() {
    assertThat(activityEntity().copy(active = true, startDate = LocalDate.MIN, endDate = LocalDate.MIN.plusDays(1)).isActiveOn(LocalDate.MIN)).isTrue
  }

  @Test
  fun `activity is inactive on closed date range when date before start of range`() {
    assertThat(activityEntity().copy(active = true, startDate = LocalDate.MIN.plusDays(1), endDate = LocalDate.MAX).isActiveOn(LocalDate.MIN)).isFalse
  }

  @Test
  fun `activity is inactive on closed date range when date at end of range`() {
    assertThat(activityEntity().copy(active = true, startDate = LocalDate.MIN, endDate = LocalDate.MIN.plusDays(1)).isActiveOn(LocalDate.MIN.plusDays(1))).isFalse
  }
}
