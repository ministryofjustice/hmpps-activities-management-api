package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.mediumPayBand
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
    payBand = lowPayBand,
  )

  private val allocationWithEndDate = allocationWithNoEndDate.copy(endDate = tomorrow, payBand = mediumPayBand)

  @Test
  fun `check allocation active status that starts today with open end date`() {
    with(allocationWithNoEndDate) {
      assertThat(isActive(yesterday)).isFalse
      assertThat(isActive(today)).isTrue
      assertThat(isActive(tomorrow)).isTrue
      assertThat(isActive(tomorrow.plusDays(1000))).isTrue
      assertThat(payBand.payBandAlias).isEqualTo("Low")
    }
  }

  @Test
  fun `check allocation active status that starts today and ends tomorrow`() {
    with(allocationWithEndDate) {
      assertThat(isActive(yesterday)).isFalse
      assertThat(isActive(today)).isTrue
      assertThat(isActive(tomorrow)).isTrue
      assertThat(isActive(tomorrow.plusDays(1))).isFalse
      assertThat(payBand.payBandAlias).isEqualTo("Medium")
    }
  }

  @Test
  fun `isUnemployment flag true when activity is within the 'non work' category`() {
    whenever(schedule.activity).thenReturn(mock())
    whenever(schedule.activity.activityCategory).thenReturn(mock())
    whenever(schedule.activity.activityCategory.code).thenReturn("SAA_NOT_IN_WORK")
    with(allocationWithEndDate) {
      assertThat(isUnemployment()).isTrue
    }
  }

  @Test
  fun `isUnemployment flag false when activity is not within the 'non work' category`() {
    whenever(schedule.activity).thenReturn(mock())
    whenever(schedule.activity.activityCategory).thenReturn(mock())
    whenever(schedule.activity.activityCategory.code).thenReturn("SAA_EDUCATION")
    with(allocationWithEndDate) {
      assertThat(isUnemployment()).isFalse
    }
  }
}
