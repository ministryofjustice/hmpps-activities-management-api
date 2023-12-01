package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import java.time.DayOfWeek

class ExclusionTest {

  @Test
  fun `setDaysOfWeek - will set day flags`() {
    val exclusion = allocation(withExclusions = true).exclusions(ExclusionsFilter.ACTIVE).first()

    exclusion.setDaysOfWeek(setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY))

    with(exclusion) {
      getDaysOfWeek() isEqualTo setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)
    }
  }
}
