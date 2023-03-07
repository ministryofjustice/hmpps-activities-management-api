package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
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
  fun `check allocation ends`() {
    with(allocation().copy(endDate = today)) {
      assertThat(ends(yesterday)).isFalse
      assertThat(ends(today)).isTrue
      assertThat(ends(tomorrow)).isFalse
    }

    with(allocation().copy(endDate = tomorrow)) {
      assertThat(ends(yesterday)).isFalse
      assertThat(ends(today)).isFalse
      assertThat(ends(tomorrow)).isTrue
    }

    with(allocation().copy(endDate = null)) {
      assertThat(ends(yesterday)).isFalse
      assertThat(ends(today)).isFalse
      assertThat(ends(tomorrow)).isFalse
    }
  }

  @Test
  fun `check can deallocate active allocation`() {
    val dateTime = LocalDateTime.now()

    assertThat(allocationWithEndDate.status(PrisonerStatus.ACTIVE)).isTrue
    assertThat(allocationWithEndDate.deallocatedReason).isNull()
    assertThat(allocationWithEndDate.deallocatedBy).isNull()
    assertThat(allocationWithEndDate.deallocatedTime).isNull()

    allocationWithEndDate.deallocate(dateTime)

    assertThat(allocationWithEndDate.status(PrisonerStatus.ENDED)).isTrue
    assertThat(allocationWithEndDate.deallocatedReason).isEqualTo("Allocation end date reached")
    assertThat(allocationWithEndDate.deallocatedBy).isEqualTo("SYSTEM")
    assertThat(allocationWithEndDate.deallocatedTime).isEqualTo(dateTime)
  }

  @Test
  fun `check cannot deallocate if allocation already ended`() {
    allocationWithEndDate.deallocate(LocalDateTime.now())

    assertThatThrownBy { allocationWithEndDate.deallocate(LocalDateTime.now()) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Allocation with ID '-1' is already deallocated.")
  }
}
