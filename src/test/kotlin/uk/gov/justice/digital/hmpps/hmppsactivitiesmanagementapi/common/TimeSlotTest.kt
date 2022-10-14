package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalTime

class TimeSlotTest {

  @Test
  fun `morning slots`() {
    assertOnHoursAndSlot(0, 11, TimeSlot.AM)
  }

  @Test
  fun `afternoon slots`() {
    assertOnHoursAndSlot(12, 16, TimeSlot.PM)
  }

  @Test
  fun `evening slots`() {
    assertOnHoursAndSlot(17, 23, TimeSlot.ED)
  }

  private fun assertOnHoursAndSlot(from: Int, to: Int, expected: TimeSlot) {
    for (hour in from..to) assertThat(TimeSlot.slot(LocalTime.of(hour, 15))).isEqualTo(expected)
  }
}
