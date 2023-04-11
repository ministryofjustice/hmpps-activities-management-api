package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import java.time.LocalDate
import java.time.LocalDateTime

class AllocationTest {

  private val today = LocalDate.now()
  private val yesterday = today.minusDays(1)
  private val tomorrow = today.plusDays(1)

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

    allocation.deallocate(dateTime, "Allocation end date reached")

    assertThat(allocation.status(PrisonerStatus.ENDED)).isTrue
    assertThat(allocation.deallocatedReason).isEqualTo("Allocation end date reached")
    assertThat(allocation.deallocatedBy).isEqualTo("SYSTEM")
    assertThat(allocation.deallocatedTime).isEqualTo(dateTime)
  }

  @Test
  fun `check cannot deallocate if allocation already ended`() {
    val allocation = allocation().apply { deallocate(LocalDateTime.now(), "reason") }

    assertThatThrownBy { allocation.deallocate(LocalDateTime.now(), "reason") }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Allocation with ID '-1' is already deallocated.")
  }

  @Test
  fun `check can suspend an active allocation`() {
    val allocation = allocation().also { assertThat(it.status(PrisonerStatus.ACTIVE)) }

    allocation.suspend(today.atStartOfDay(), "Temporarily released from prison")

    with(allocation) {
      assertThat(prisonerStatus).isEqualTo(PrisonerStatus.AUTO_SUSPENDED)
      assertThat(suspendedBy).isEqualTo("SYSTEM")
      assertThat(suspendedTime).isEqualTo(today.atStartOfDay())
      assertThat(suspendedReason).isEqualTo("Temporarily released from prison")
    }
  }

  @Test
  fun `check cannot suspend an already inactive allocation`() {
    val allocation = allocation().apply { deallocate(LocalDateTime.now(), "reason") }.also { assertThat(it.status(PrisonerStatus.ENDED)) }

    assertThatThrownBy { allocation.suspend(today.atStartOfDay(), "Temporarily released from prison") }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only suspend active allocations")
  }

  @Test
  fun `check cannot supend same allocation`() {
    val allocation = allocation().also { assertThat(it.status(PrisonerStatus.ACTIVE)) }

    allocation.suspend(today.atStartOfDay(), "Temporarily released from prison")

    assertThatThrownBy { allocation.suspend(today.atStartOfDay(), "Temporarily released from prison") }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("You can only suspend active allocations")
  }
}
