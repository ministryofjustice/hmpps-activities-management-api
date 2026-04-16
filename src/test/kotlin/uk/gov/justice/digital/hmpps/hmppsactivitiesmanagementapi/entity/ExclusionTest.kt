package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import java.time.DayOfWeek
import java.time.LocalDate

class ExclusionTest {

  @Test
  fun `setDaysOfWeek - will set day flags`() {
    val exclusion = allocation(withExclusions = true).exclusions(ExclusionsFilter.ACTIVE).first()

    exclusion.setDaysOfWeek(setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY))

    with(exclusion) {
      getDaysOfWeek() isEqualTo setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)
    }
  }

  @Test
  fun `endYesterday - will fail if start date is today`() {
    val exclusion = allocation(startDate = LocalDate.now(), withExclusions = true).exclusions(ExclusionsFilter.ACTIVE).first()

    assertThatThrownBy { exclusion.endYesterday() }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Can only end an exclusion for yesterday if exclusion started in the past")
  }

  @Test
  fun `endYesterday - will fail if start date is in future`() {
    val exclusion = allocation(startDate = LocalDate.now().plusDays(1), withExclusions = true).exclusions(ExclusionsFilter.ACTIVE).first()

    assertThatThrownBy { exclusion.endYesterday() }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Can only end an exclusion for yesterday if exclusion started in the past")
  }

  @Test
  fun `endYesterday - will set end date to yesterday if start date is in the past`() {
    val yesterday = LocalDate.now().minusDays(1)
    val exclusion = allocation(startDate = yesterday, withExclusions = true).exclusions(ExclusionsFilter.ACTIVE).first()

    exclusion.endYesterday()

    with(exclusion) {
      endDate isEqualTo yesterday
    }
  }
}
