import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.ActivityCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.ActivityUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerAddedToWaitingListEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerAllocatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerDeallocatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerDeclinedFromWaitingListEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerRemovedFromWaitingListEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity as EntityActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation as EntityAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList as EntityWaitingList

fun EntityActivity.toActivityCreatedEvent() = ActivityCreatedEvent(
  activityId = activityId,
  activityName = summary,
  prisonCode = prisonCode,
  categoryCode = activityCategory.name,
  startDate = startDate,
  createdAt = createdTime,
)

fun EntityActivity.toActivityUpdatedEvent() = ActivityUpdatedEvent(
  activityId = activityId,
  activityName = summary,
  prisonCode = prisonCode,
  categoryCode = activityCategory.name,
  startDate = startDate,
  createdAt = updatedTime!!,
)

fun EntityAllocation.toPrisonerAllocatedEvent(waitingListId: Long? = null) = PrisonerAllocatedEvent(
  activityId = activitySchedule.activity.activityId,
  activityName = activitySchedule.activity.summary,
  prisonCode = activitySchedule.activity.prisonCode,
  prisonerNumber = prisonerNumber,
  scheduleId = activitySchedule.activityScheduleId,
  scheduleDescription = activitySchedule.description,
  waitingListId = waitingListId,
  createdAt = allocatedTime,
)

fun EntityAllocation.toPrisonerDeallocatedEvent() = let {
  if (isEnded().not() || deallocatedTime == null || deallocatedReason == null || deallocatedBy == null) {
    throw IllegalStateException("Prisoner $prisonerNumber is missing expected deallocation details for allocation id 123456")
  }

  PrisonerDeallocatedEvent(
    activityId = activitySchedule.activity.activityId,
    activityName = activitySchedule.activity.summary,
    prisonCode = activitySchedule.activity.prisonCode,
    prisonerNumber = prisonerNumber,
    scheduleId = activitySchedule.activityScheduleId,
    deallocationTime = deallocatedTime!!,
    reason = deallocatedReason!!.description,
    deallocatedBy = deallocatedBy!!,
  )
}

fun EntityWaitingList.toPrisonerAddedToWaitingListEvent() = PrisonerAddedToWaitingListEvent(
  activityId = activity.activityId,
  scheduleId = activitySchedule.activityScheduleId,
  activityName = activity.summary,
  prisonCode = activity.prisonCode,
  prisonerNumber = prisonerNumber,
  status = status,
  createdBy = createdBy,
  createdAt = creationTime,
)

fun EntityWaitingList.toPrisonerDeclinedFromWaitingListEvent() = PrisonerDeclinedFromWaitingListEvent(
  waitingListId = waitingListId,
  activityId = activity.activityId,
  scheduleId = activitySchedule.activityScheduleId,
  activityName = activity.summary,
  prisonCode = activity.prisonCode,
  prisonerNumber = prisonerNumber,
  declinedBy = updatedBy ?: createdBy,
  declinedAt = updatedTime ?: creationTime,
)

fun EntityWaitingList.toPrisonerRemovedFromWaitingListEvent() = PrisonerRemovedFromWaitingListEvent(
  waitingListId = waitingListId,
  activityId = activity.activityId,
  scheduleId = activitySchedule.activityScheduleId,
  activityName = activity.summary,
  prisonCode = activity.prisonCode,
  prisonerNumber = prisonerNumber,
  removedBy = updatedBy ?: createdBy,
  removedAt = updatedTime ?: creationTime,
)
