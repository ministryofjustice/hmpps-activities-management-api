package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
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

    with(waitingList().apply { allocated(allocation) }) {
      assertThat(this.status).isEqualTo(WaitingListStatus.ALLOCATED)
      assertThat(this.allocation).isEqualTo(allocation)
    }
  }
}
