package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PrisonRegimeTest {

  private val fiveDaysToExpiry = 5

  private val regime = PrisonRegime(
    1,
    prisonCode = MOORLAND_PRISON_CODE,
    amStart = LocalTime.MIDNIGHT,
    amFinish = LocalTime.MIDNIGHT.plusHours(1),
    pmStart = LocalTime.NOON,
    pmFinish = LocalTime.NOON.plusHours(1),
    edStart = LocalTime.NOON.plusHours(5),
    edFinish = LocalTime.NOON.plusHours(10),
    maxDaysToExpiry = fiveDaysToExpiry,
  )

  @Test
  fun `active allocation has not expired`() {
    val activeAllocation = allocation()

    assertThat(regime.hasExpired(activeAllocation)).isFalse
  }

  @Test
  fun `user suspended allocation has not expired`() {
    val userSuspendedAllocation = allocation(withPlannedSuspensions = true).activatePlannedSuspension()

    assertThat(regime.hasExpired(userSuspendedAllocation)).isFalse
  }

  @Test
  fun `user suspended allocation 5 days old has not expired`() {
    val userSuspendedAllocation = allocation(withPlannedSuspensions = true).activatePlannedSuspension()

    assertThat(regime.hasExpired(userSuspendedAllocation)).isFalse
  }

  @Test
  fun `auto suspended allocation 4 days has not expired`() {
    val userSuspendedAllocation = allocation().autoSuspend(LocalDateTime.now().minusDays(4), "reason")

    assertThat(regime.hasExpired(userSuspendedAllocation)).isFalse
  }

  @Test
  fun `auto suspended allocation 5 days has expired`() {
    val userSuspendedAllocation =
      allocation().autoSuspend(LocalDateTime.now().minusDays(fiveDaysToExpiry.toLong()), "reason")

    assertThat(regime.hasExpired(userSuspendedAllocation)).isTrue
  }

  @Test
  fun `date has not expired`() {
    assertThat(regime.hasExpired { LocalDate.now() }).isFalse
  }

  @Test
  fun `date has expired`() {
    assertThat(regime.hasExpired { LocalDate.now().minusDays(fiveDaysToExpiry.toLong()) }).isTrue
  }
}
