package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import java.time.LocalDateTime

class AllocationTest {

  private val today = TimeSource.today()
  private val yesterday = TimeSource.yesterday()
  private val tomorrow = TimeSource.tomorrow()

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
    val allocation = allocation()

    assertThat(allocation.status(PrisonerStatus.ACTIVE)).isTrue
    assertThat(allocation.deallocatedReason).isNull()
    assertThat(allocation.deallocatedBy).isNull()
    assertThat(allocation.deallocatedTime).isNull()

    allocation.deallocateNow(dateTime, DeallocationReason.ENDED)

    assertThat(allocation.status(PrisonerStatus.ENDED)).isTrue
    assertThat(allocation.deallocatedReason).isEqualTo(DeallocationReason.ENDED)
    assertThat(allocation.deallocatedBy).isEqualTo("Activities Management Service")
    assertThat(allocation.deallocatedTime).isEqualTo(dateTime)
  }

  @Test
  fun `check cannot deallocate if allocation already ended`() {
    val allocation = allocation().apply { deallocateNow(LocalDateTime.now(), DeallocationReason.ENDED) }

    assertThatThrownBy { allocation.deallocateNow(LocalDateTime.now(), DeallocationReason.ENDED) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Allocation with ID '0' is already deallocated.")
  }

  @Test
  fun `check can auto-suspend an active allocation`() {
    val allocation = allocation().also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    allocation.autoSuspend(today.atStartOfDay(), "Temporarily released from prison")

    with(allocation) {
      assertThat(prisonerStatus).isEqualTo(PrisonerStatus.AUTO_SUSPENDED)
      assertThat(suspendedBy).isEqualTo("Activities Management Service")
      assertThat(suspendedTime).isEqualTo(today.atStartOfDay())
      assertThat(suspendedReason).isEqualTo("Temporarily released from prison")
    }
  }

  @Test
  fun `check cannot auto-suspend an ended allocation`() {
    val allocation = allocation().apply { deallocateNow(LocalDateTime.now(), DeallocationReason.ENDED) }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ENDED) }

    assertThatThrownBy { allocation.autoSuspend(today.atStartOfDay(), "Temporarily released from prison") }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only suspend active allocations")
  }

  @Test
  fun `check cannot auto-suspend same allocation`() {
    val allocation = allocation().also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    allocation.autoSuspend(today.atStartOfDay(), "Temporarily released from prison")

    assertThatThrownBy { allocation.autoSuspend(today.atStartOfDay(), "Temporarily released from prison") }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only suspend active allocations")
  }

  @Test
  fun `check can unsuspend an auto-suspended allocation`() {
    val allocation = allocation()
      .apply { autoSuspend(today.atStartOfDay(), "Temporarily released from prison") }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.AUTO_SUSPENDED) }

    allocation.reactivateAutoSuspensions()

    with(allocation) {
      assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE)
      assertThat(suspendedBy).isNull()
      assertThat(suspendedTime).isNull()
      assertThat(suspendedReason).isNull()
    }
  }

  @Test
  fun `check cannot unsuspend a manual user suspended allocation`() {
    val allocation = allocation()
      .apply { userSuspend(today.atStartOfDay(), "User suspension", "A user") }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.SUSPENDED) }

    assertThatThrownBy { allocation.reactivateAutoSuspensions() }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only reactivate auto-suspended allocations")
  }

  @Test
  fun `check cannot unsuspend an active allocation`() {
    val allocation = allocation().also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    assertThatThrownBy { allocation.reactivateAutoSuspensions() }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only reactivate auto-suspended allocations")
  }

  @Test
  fun `check cannot unsuspend an ended allocation`() {
    val allocation = allocation().apply { deallocateNow(LocalDateTime.now(), DeallocationReason.ENDED) }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ENDED) }

    assertThatThrownBy { allocation.reactivateAutoSuspensions() }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only reactivate auto-suspended allocations")
  }

//  If the activity (has an end date) the allocation is associated with then planned date cannot exceed this.

  @Test
  fun `planned deallocation must be in the future`() {
    val allocation = allocation().also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    assertThatThrownBy {
      allocation.deallocateOn(today, DeallocationReason.PERSONAL, "by test")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Planned deallocation date must be in the future.")
  }

  @Test
  fun `planned deallocation must be on or before the allocation end date`() {
    val allocationEndingToday =
      allocation().copy(endDate = today).also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    assertThatThrownBy {
      allocationEndingToday.deallocateOn(tomorrow, DeallocationReason.PERSONAL, "by test")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Planned date cannot be after $today.")
  }

  @Test
  fun `planned deallocation must be on or before the schedule end date`() {
    val scheduleEndingToday: ActivitySchedule = mock { on { endDate } doReturn today }

    val allocationNoEndDate = allocation().copy(endDate = null, activitySchedule = scheduleEndingToday)
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    assertThatThrownBy {
      allocationNoEndDate.deallocateOn(tomorrow, DeallocationReason.PERSONAL, "by test")
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Planned date cannot be after $today.")
  }

  @Test
  fun `cannot plan deallocation if already planned`() {
    val allocation = allocation().copy(allocationId = 1, endDate = null)
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    allocation.deallocateOn(tomorrow, DeallocationReason.PERSONAL, "by test")

    assertThatThrownBy {
      allocation.deallocateOn(tomorrow, DeallocationReason.PERSONAL, "by test")
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Allocation with ID '1' is already planned.")
  }

  @Test
  fun `cannot plan deallocation if already ended`() {
    val allocation = allocation().copy(allocationId = 1, endDate = null)
      .apply { deallocateNow(TimeSource.now(), DeallocationReason.ENDED) }
      .also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ENDED) }

    assertThatThrownBy {
      allocation.deallocateOn(tomorrow, DeallocationReason.PERSONAL, "by test")
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Allocation with ID '1' is already deallocated.")
  }
}
