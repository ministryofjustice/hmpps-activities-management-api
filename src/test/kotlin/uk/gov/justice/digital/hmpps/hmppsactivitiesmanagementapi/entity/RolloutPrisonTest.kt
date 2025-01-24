package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService.Companion.hasExpired
import java.time.LocalDate

class RolloutPrisonTest {

  private val rolloutPrison = RolloutPrisonPlan(
    prisonCode = "",
    maxDaysToExpiry = 5,
    activitiesRolledOut = true,
    appointmentsRolledOut = true,
    prisonLive = true,
  )

  @Test
  fun `date has not expired`() {
    assertThat(rolloutPrison.hasExpired { LocalDate.now() }).isFalse
  }

  @Test
  fun `date has expired`() {
    assertThat(rolloutPrison.hasExpired { LocalDate.now().minusDays(22) }).isTrue
  }
}
