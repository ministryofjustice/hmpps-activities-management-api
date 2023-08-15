package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList

class WaitingListTest {

  @Test
  fun `isStatus checks the status of a waiting list application`() {
    with(waitingList().apply { status = WaitingListStatus.PENDING }) {
      assertThat(isStatus(WaitingListStatus.PENDING, WaitingListStatus.APPROVED)).isTrue
      assertThat(isStatus(WaitingListStatus.APPROVED)).isFalse
    }
  }

  @Test
  fun `allocated sets the status of a waiting list application to ALLOCATED and sets the allocation`() {
    val allocation = allocation()

    with(
      waitingList(
        prisonCode = allocation.prisonCode(),
        prisonerNumber = allocation.prisonerNumber,
      ).apply { allocated(allocation) },
    ) {
      assertThat(this.status).isEqualTo(WaitingListStatus.ALLOCATED)
      assertThat(this.allocation).isEqualTo(allocation)
    }
  }

  @Test
  fun `allocated fails if already allocated`() {
    val allocation = allocation()

    val waitingList =
      waitingList(prisonCode = allocation.prisonCode(), prisonerNumber = allocation.prisonerNumber).apply {
        allocated(allocation)
      }

    assertThatThrownBy { waitingList.allocated(allocation) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Waiting list ${waitingList.waitingListId} is already allocated")
  }

  @Test
  fun `allocated fails if allocation belongs to a different prisoner`() {
    val allocation = allocation().copy(prisonerNumber = "XYZ")
    val waitingList = waitingList(prisonerNumber = "ABC", prisonCode = allocation.prisonCode())

    assertThatThrownBy {
      waitingList.allocated(allocation)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation ${allocation.allocationId} prisoner number does not match with waiting list ${waitingList.waitingListId}")
  }

  @Test
  fun `allocated fails if allocation belongs to a different prison`() {
    val allocation = allocation().copy(prisonerNumber = "ABC")
    val waitingList = waitingList(prisonCode = allocation.prisonCode().plus("X"), prisonerNumber = "ABC")

    assertThatThrownBy {
      waitingList.allocated(allocation)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation ${allocation.allocationId} prison does not match with waiting list ${waitingList.waitingListId}")
  }
}
