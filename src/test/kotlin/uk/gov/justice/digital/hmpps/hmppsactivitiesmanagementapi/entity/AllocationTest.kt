package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonPayBands
import java.time.LocalDate
import java.time.LocalDateTime

class AllocationTest {

  private val schedule: ActivitySchedule = mock()
  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val tomorrow = today.plusDays(1)

  private val allocationWithNoEndDate = Allocation(
    activitySchedule = schedule,
    prisonerNumber = "1234567890",
    startDate = today,
    allocatedBy = "FAKE USER",
    allocatedTime = LocalDateTime.now(),
    payBand = prisonPayBands().first()
  )

  private val allocationWithEndDate = allocationWithNoEndDate.copy(endDate = tomorrow)

  @Test
  fun `check allocation active status that starts today with open end date`() {
    with(allocationWithNoEndDate) {
      assertThat(isActive(yesterday)).isFalse
      assertThat(isActive(today)).isTrue
      assertThat(isActive(tomorrow)).isTrue
      assertThat(isActive(tomorrow.plusDays(1000))).isTrue
    }
  }

  @Test
  fun `check allocation active status that starts today and ends tomorrow`() {
    with(allocationWithEndDate) {
      assertThat(isActive(yesterday)).isFalse
      assertThat(isActive(today)).isTrue
      assertThat(isActive(tomorrow)).isTrue
      assertThat(isActive(tomorrow.plusDays(1))).isFalse
    }
  }
}
