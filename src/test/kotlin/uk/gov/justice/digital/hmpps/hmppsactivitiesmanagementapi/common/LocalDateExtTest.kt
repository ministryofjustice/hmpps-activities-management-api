package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import java.time.LocalDate

class LocalDateExtTest {

  @Nested
  @DisplayName("between")
  inner class Between {
    @Test
    fun `returns false if below range`() {
      val date = LocalDate.of(2022, 1, 1)
      val dayAfter = date.plusDays(1)
      val weekAfter = date.plusWeeks(1)
      date.between(dayAfter, weekAfter) isBool false
    }

    @Test
    fun `returns true if on lower range`() {
      val date = LocalDate.of(2022, 1, 1)
      val weekAfter = date.plusWeeks(1)
      date.between(date, weekAfter) isBool true
    }

    @Test
    fun `returns true if in range`() {
      val date = LocalDate.of(2022, 1, 1)
      val dayBefore = date.minusDays(1)
      val weekAfter = date.plusWeeks(1)
      date.between(dayBefore, weekAfter) isBool true
    }

    @Test
    fun `returns true if on upper range`() {
      val date = LocalDate.of(2022, 1, 1)
      val dayBefore = date.minusDays(1)
      val weekAfter = date.plusWeeks(1)
      weekAfter.between(dayBefore, weekAfter) isBool true
    }

    @Test
    fun `returns true if no upper range`() {
      val date = LocalDate.of(2022, 1, 1)
      val dayBefore = date.minusDays(1)
      date.between(dayBefore, null) isBool true
    }

    @Test
    fun `returns false if outside upper range`() {
      val date = LocalDate.of(2022, 1, 1)
      val dayBefore = date.minusDays(1)
      val weekAfter = date.plusWeeks(1)
      weekAfter.between(dayBefore, date) isBool false
    }
  }

  @Nested
  @DisplayName("onOrBefore")
  inner class OnOrBefore {
    @Test
    fun `returns true if date is on`() {
      val date = LocalDate.of(2022, 1, 1)
      date.onOrBefore(date) isBool true
    }

    @Test
    fun `returns true if date is before`() {
      val date = LocalDate.of(2022, 1, 1)
      val dayAfter = date.plusDays(1)
      date.onOrBefore(dayAfter) isBool true
    }

    @Test
    fun `returns false if date is after`() {
      val date = LocalDate.of(2022, 1, 1)
      val dayBefore = date.minusDays(1)
      date.onOrBefore(dayBefore) isBool false
    }
  }

  @Nested
  @DisplayName("onOrAfter")
  inner class OnOrAfter {
    @Test
    fun `returns true if date is on`() {
      val date = LocalDate.of(2022, 1, 1)
      date.onOrAfter(date) isBool true
    }

    @Test
    fun `returns true if date is after`() {
      val date = LocalDate.of(2022, 1, 1)
      val dayBefore = date.minusDays(1)
      date.onOrAfter(dayBefore) isBool true
    }

    @Test
    fun `returns false if date is after`() {
      val date = LocalDate.of(2022, 1, 1)
      val dayAfter = date.plusDays(1)
      date.onOrAfter(dayAfter) isBool false
    }
  }

  @Nested
  @DisplayName("toMediumFormatStyle")
  inner class ToMediumFormatStyle {
    @Test
    fun `to medium format style`() {
      LocalDate.of(2024, 1, 1).toMediumFormatStyle() isEqualTo "1 Jan 2024"
    }
  }

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
