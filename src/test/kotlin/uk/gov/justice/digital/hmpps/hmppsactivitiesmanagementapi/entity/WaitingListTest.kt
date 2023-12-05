package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
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
    val allocation = allocation().copy(allocatedPrisonerNumber = "XYZ")
    val waitingList = waitingList(prisonerNumber = "ABC", prisonCode = allocation.prisonCode())

    assertThatThrownBy {
      waitingList.allocated(allocation)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation ${allocation.allocationId} prisoner number does not match with waiting list ${waitingList.waitingListId}")
  }

  @Test
  fun `allocated fails if allocation belongs to a different prison`() {
    val allocation = allocation().copy(allocatedPrisonerNumber = "ABC")
    val waitingList = waitingList(prisonCode = allocation.prisonCode().plus("X"), prisonerNumber = "ABC")

    assertThatThrownBy {
      waitingList.allocated(allocation)
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Allocation ${allocation.allocationId} prison does not match with waiting list ${waitingList.waitingListId}")
  }

  @Test
  fun `can decline pending waiting list`() {
    val waitingList = waitingList(initialStatus = WaitingListStatus.PENDING)
    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.declinedReason isEqualTo null

    waitingList.status = WaitingListStatus.DECLINED
    waitingList.declinedReason = "Released"

    with(waitingList) {
      status isEqualTo WaitingListStatus.DECLINED
      declinedReason isEqualTo "Released"
    }
  }

  @Test
  fun `can decline approved waiting list`() {
    val waitingList = waitingList(initialStatus = WaitingListStatus.APPROVED)
    waitingList.status isEqualTo WaitingListStatus.APPROVED
    waitingList.declinedReason isEqualTo null

    waitingList.status = WaitingListStatus.DECLINED
    waitingList.declinedReason = "Released"

    with(waitingList) {
      status isEqualTo WaitingListStatus.DECLINED
      declinedReason isEqualTo "Released"
    }
  }

  @Test
  fun `cannot decline allocated waiting list`() {
    val waitingList = waitingList(initialStatus = WaitingListStatus.ALLOCATED)
    waitingList.status isEqualTo WaitingListStatus.ALLOCATED
    waitingList.declinedReason isEqualTo null

    assertThatThrownBy {
      waitingList.status = WaitingListStatus.DECLINED
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Only pending and approved waiting lists can be declined")

    with(waitingList) {
      status isEqualTo WaitingListStatus.ALLOCATED
      declinedReason isEqualTo null
    }
  }

  @Test
  fun `can decline removed waiting list`() {
    val waitingList = waitingList(initialStatus = WaitingListStatus.REMOVED)
    waitingList.status isEqualTo WaitingListStatus.REMOVED
    waitingList.declinedReason isEqualTo null

    assertThatThrownBy {
      waitingList.status = WaitingListStatus.DECLINED
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Only pending and approved waiting lists can be declined")

    with(waitingList) {
      status isEqualTo WaitingListStatus.REMOVED
      declinedReason isEqualTo null
    }
  }

  @Test
  fun `can only set declined reason for declined waiting lists`() {
    val waitingList = waitingList(initialStatus = WaitingListStatus.REMOVED)
    waitingList.status isEqualTo WaitingListStatus.REMOVED
    waitingList.declinedReason isEqualTo null

    assertThatThrownBy {
      waitingList.declinedReason = "This should not be set"
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot set the declined reason when status is not declined")

    with(waitingList) {
      status isEqualTo WaitingListStatus.REMOVED
      declinedReason isEqualTo null
    }
  }

  @Test
  fun `can approve declined waiting list`() {
    val waitingList = waitingList(initialStatus = WaitingListStatus.DECLINED).apply { declinedReason = "Declined" }
    waitingList.status isEqualTo WaitingListStatus.DECLINED
    waitingList.declinedReason isEqualTo "Declined"

    waitingList.status = WaitingListStatus.APPROVED

    with(waitingList) {
      status isEqualTo WaitingListStatus.APPROVED
      declinedReason isEqualTo null
    }
  }

  @Test
  fun `can change declined waiting list to pending`() {
    val waitingList = waitingList(initialStatus = WaitingListStatus.DECLINED).apply { declinedReason = "Declined" }
    waitingList.status isEqualTo WaitingListStatus.DECLINED
    waitingList.declinedReason isEqualTo "Declined"

    waitingList.status = WaitingListStatus.PENDING

    with(waitingList) {
      status isEqualTo WaitingListStatus.PENDING
      declinedReason isEqualTo null
    }
  }
}
