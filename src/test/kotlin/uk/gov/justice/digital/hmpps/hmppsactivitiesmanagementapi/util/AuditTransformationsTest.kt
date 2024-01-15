package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import toPrisonerAddedToWaitingListEvent
import toPrisonerDeallocatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class AuditTransformationsTest {

  @Test
  fun `transform to prisoner deallocated event`() {
    val allocation = allocation().deallocateNow()

    with(allocation.toPrisonerDeallocatedEvent()) {
      assertThat(activityId).isEqualTo(allocation.activitySchedule.activity.activityId)
      assertThat(activityName).isEqualTo(allocation.activitySchedule.activity.summary)
      assertThat(scheduleId).isEqualTo(allocation.activitySchedule.activityScheduleId)
      assertThat(prisonCode).isEqualTo(allocation.activitySchedule.activity.prisonCode)
      assertThat(prisonerNumber).isEqualTo(allocation.prisonerNumber)
      assertThat(deallocatedBy).isEqualTo(allocation.deallocatedBy)
      assertThat(deallocationTime).isEqualTo(allocation.deallocatedTime)
      assertThat(reason).isEqualTo(allocation.deallocatedReason?.description)
      assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
    }
  }

  @Test
  fun `fails to transform to prisoner deallocated event if not deallocated`() {
    assertThatThrownBy {
      allocation().copy(allocationId = 123456, prisonerNumber = "ABCDEF").toPrisonerDeallocatedEvent()
    }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Prisoner ABCDEF is missing expected deallocation details for allocation id 123456")
  }

  @Test
  fun `transform to prisoner added to waiting list event`() {
    val waitingList = waitingList(prisonCode = MOORLAND_PRISON_CODE)

    with(waitingList.toPrisonerAddedToWaitingListEvent()) {
      activityId isEqualTo waitingList.activity.activityId
      activityName isEqualTo waitingList.activity.summary
      scheduleId isEqualTo waitingList.activitySchedule.activityScheduleId
      prisonCode isEqualTo waitingList.activity.prisonCode
      prisonerNumber isEqualTo waitingList.prisonerNumber
      status isEqualTo waitingList.status
      createdAt isEqualTo waitingList.creationTime
    }
  }
}
