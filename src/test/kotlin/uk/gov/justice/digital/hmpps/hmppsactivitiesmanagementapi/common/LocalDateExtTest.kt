package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import java.time.LocalDate

class LocalDateExtTest {

  @Test
  fun `days ago`() {
    (1..10).forEach { it.daysAgo() isEqualTo LocalDate.now().minusDays(it.toLong()) }
  }

  @Test
  fun `days ago fails if not a positive number`() {
    (0 downTo -10).forEach {
      assertThatThrownBy {
        (it).daysAgo()
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Days ago must be positive")
    }
  }

  @Test
  fun `weeks ago`() {
    (1..10).forEach { it.weeksAgo() isEqualTo LocalDate.now().minusWeeks(it.toLong()) }
  }

  @Test
  fun `weeks ago fails if not a positive number`() {
    (0 downTo -10).forEach {
      assertThatThrownBy {
        (it).weeksAgo()
      }.isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Weeks ago must be positive")
    }
  }

  @Test
  fun `on or before`() {
    TimeSource.yesterday().onOrBefore(TimeSource.yesterday()) isBool true
    TimeSource.yesterday().onOrBefore(TimeSource.today()) isBool true
    TimeSource.yesterday().onOrBefore(TimeSource.tomorrow()) isBool true

    TimeSource.today().onOrBefore(TimeSource.today()) isBool true
    TimeSource.today().onOrBefore(TimeSource.tomorrow()) isBool true
    TimeSource.today().onOrBefore(TimeSource.yesterday()) isBool false

    TimeSource.tomorrow().onOrBefore(TimeSource.tomorrow()) isBool true
    TimeSource.tomorrow().onOrBefore(TimeSource.yesterday()) isBool false

    val nullDate: LocalDate? = null
    nullDate.onOrBefore(TimeSource.yesterday()) isBool false
    nullDate.onOrBefore(TimeSource.today()) isBool false
    nullDate.onOrBefore(TimeSource.tomorrow()) isBool false
  }
}
