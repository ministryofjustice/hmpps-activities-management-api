import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.ActivityCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.ActivityUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerAllocatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerDeallocatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity as EntityActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation as EntityAllocation

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

fun EntityAllocation.toPrisonerAllocatedEvent() = PrisonerAllocatedEvent(
  activityId = activitySchedule.activity.activityId,
  activityName = activitySchedule.activity.summary,
  prisonCode = activitySchedule.activity.prisonCode,
  prisonerNumber = prisonerNumber,
  scheduleId = activitySchedule.activityScheduleId,
  scheduleDescription = activitySchedule.description,
  createdAt = allocatedTime,
)

fun EntityAllocation.toPrisonerDeallocatedEvent() =
  let {
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
