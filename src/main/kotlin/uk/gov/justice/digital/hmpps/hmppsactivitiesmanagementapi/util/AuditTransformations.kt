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

fun EntityAllocation.toPrisonerDeallocatedEvent() = PrisonerDeallocatedEvent(
  activityId = activitySchedule.activity.activityId,
  activityName = activitySchedule.activity.summary,
  prisonCode = activitySchedule.activity.prisonCode,
  prisonerNumber = prisonerNumber,
  scheduleId = activitySchedule.activityScheduleId,
  deallocationTime = deallocatedTime ?: throw IllegalStateException("Missing expected deallocation time"),
  reason = deallocatedReason?.description ?: throw IllegalStateException("Missing expected deallocation description"),
  deallocatedBy = deallocatedBy ?: throw IllegalStateException("Missing expected deallocated By"),
)
