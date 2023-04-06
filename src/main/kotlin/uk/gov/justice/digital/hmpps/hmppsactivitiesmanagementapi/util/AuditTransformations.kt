import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.ActivityCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerAllocatedEvent
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

fun EntityAllocation.toPrisonerAllocatedEvent() = PrisonerAllocatedEvent(

  activityId = activitySchedule.activity.activityId,
  activityName = activitySchedule.activity.summary,
  prisonCode = activitySchedule.activity.prisonCode,
  prisonerNumber = prisonerNumber,
  scheduleId = activitySchedule.activityScheduleId,
  scheduleDescription = activitySchedule.description,
  createdAt = allocatedTime,
)
