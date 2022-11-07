package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import java.time.format.TextStyle
import java.util.Locale
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity as EntityActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory as EntityActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityEligibility as EntityActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPay as EntityActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule as EntityActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSuspension as EntitySuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier as EntityActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation as EntityAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance as EntityAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerWaiting as EntityPrisonerWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison as EntityRolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance as EntityScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityCategory as ModelActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityEligibility as ModelActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite as ModelActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance as ModelActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite as ModelActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier as ModelActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation as ModelAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason as ModelAttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EligibilityRule as ModelEligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation as ModelInternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerWaiting as ModelPrisonerWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrison as ModelRolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstance as ModelScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Suspension as ModelSuspension

/**
 * Transform functions providing a thin layer to transform entities into their API model equivalents and vice-versa.
 */
fun transform(activity: EntityActivity) =
  ModelActivity(
    id = activity.activityId!!,
    prisonCode = activity.prisonCode,
    category = activity.activityCategory.toModelActivityCategory(),
    tier = activity.activityTier.toModelActivityTier(),
    eligibilityRules = activity.eligibilityRules.toModelEligibilityRules(),
    schedules = activity.schedules.toModelSchedules(),
    waitingList = activity.waitingList.toModelWaitingList(),
    pay = activity.activityPay.toModelActivityPayList(),
    summary = activity.summary,
    description = activity.description,
    startDate = activity.startDate,
    endDate = activity.endDate,
    active = activity.active,
    createdTime = activity.createdTime,
    createdBy = activity.createdBy
  )

fun transformActivityScheduleInstances(scheduledInstances: List<EntityScheduledInstance>): List<ModelActivityScheduleInstance> =
  scheduledInstances.toModelActivityScheduleInstances()

private fun EntityActivityCategory.toModelActivityCategory() =
  ModelActivityCategory(
    this.activityCategoryId!!,
    this.code,
    this.description
  )

private fun EntityActivityTier.toModelActivityTier() =
  ModelActivityTier(
    id = this.activityTierId!!,
    code = this.code,
    description = this.description,
  )

private fun List<EntityActivityEligibility>.toModelEligibilityRules() = map {
  ModelActivityEligibility(
    it.activityEligibilityId!!,
    it.eligibilityRule.let { er -> ModelEligibilityRule(er.eligibilityRuleId!!, er.code, er.description) }
  )
}

fun transform(scheduleEntities: List<EntityActivitySchedule>) = scheduleEntities.toModelSchedules()

private fun List<EntityActivitySchedule>.toModelSchedules() = map {
  ModelActivitySchedule(
    id = it.activityScheduleId!!,
    instances = it.instances.toModelScheduledInstances(),
    allocations = it.allocations.toModelAllocations(),
    description = it.description,
    suspensions = it.suspensions.toModelSuspensions(),
    startTime = it.startTime,
    endTime = it.endTime,
    internalLocation = it.toInternalLocation(),
    capacity = it.capacity,
    daysOfWeek = it.getDaysOfWeek().map { day -> day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) }
  )
}

private fun List<EntityPrisonerWaiting>.toModelWaitingList() = map {
  ModelPrisonerWaiting(
    id = it.prisonerWaitingId!!,
    prisonerNumber = it.prisonerNumber,
    priority = it.priority,
    createdTime = it.createdTime,
    createdBy = it.createdBy,
  )
}

private fun List<EntityScheduledInstance>.toModelScheduledInstances() = map {
  ModelScheduledInstance(
    id = it.scheduledInstanceId!!,
    date = it.sessionDate,
    startTime = it.startTime,
    endTime = it.endTime,
    cancelled = it.cancelled,
    cancelledTime = it.cancelledTime,
    cancelledBy = it.cancelledBy,
    attendances = it.attendances.map { attendance -> transform(attendance) }
  )
}

private fun List<EntityScheduledInstance>.toModelActivityScheduleInstances() = map {
  ModelActivityScheduleInstance(
    activitySchedule = it.activitySchedule.toModelActivityScheduleLite(),
    id = it.scheduledInstanceId,
    date = it.sessionDate,
    startTime = it.startTime,
    endTime = it.endTime,
    cancelled = it.cancelled,
    cancelledTime = it.cancelledTime,
    cancelledBy = it.cancelledBy,
  )
}

private fun EntityActivitySchedule.toModelActivityScheduleLite() =
  ModelActivityScheduleLite(
    id = this.activityScheduleId!!,
    description = this.description,
    startTime = this.startTime,
    endTime = this.endTime,
    internalLocation = this.toInternalLocation(),
    capacity = this.capacity,
    daysOfWeek = this.getDaysOfWeek().map { day -> day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) },
    activity = this.activity.toModelActivityLite()
  )

private fun EntityActivity.toModelActivityLite() =
  ModelActivityLite(
    id = this.activityId!!,
    prisonCode = this.prisonCode,
    summary = this.summary,
    description = this.description,
    active = this.active
  )

private fun List<EntityAllocation>.toModelAllocations() = map {
  ModelAllocation(
    id = it.allocationId!!,
    prisonerNumber = it.prisonerNumber,
    incentiveLevel = it.incentiveLevel,
    payBand = it.payBand,
    startDate = it.startDate,
    endDate = it.endDate,
    active = it.active,
    allocatedTime = it.allocatedTime,
    allocatedBy = it.allocatedBy
  )
}

private fun List<EntitySuspension>.toModelSuspensions() = map {
  ModelSuspension(
    suspendedFrom = it.suspendedFrom,
    suspendedUntil = it.suspendedUntil
  )
}

private fun List<EntityActivityPay>.toModelActivityPayList() = map {
  ModelActivityPay(
    id = it.activityPayId!!,
    incentiveLevel = it.incentiveLevel,
    payBand = it.payBand,
    rate = it.rate,
    pieceRate = it.pieceRate,
    pieceRateItems = it.pieceRateItems
  )
}

private fun EntityActivitySchedule.toInternalLocation() = internalLocationId?.let {
  ModelInternalLocation(
    id = internalLocationId!!,
    code = internalLocationCode!!,
    description = internalLocationDescription!!
  )
}

fun transform(prison: EntityRolloutPrison) = ModelRolloutPrison(
  id = prison.rolloutPrisonId!!,
  code = prison.code,
  description = prison.description,
  active = prison.active
)

fun transform(attendance: EntityAttendance): ModelAttendance =
  ModelAttendance(
    id = attendance.attendanceId!!,
    prisonerNumber = attendance.prisonerNumber,
    attendanceReason = attendance.attendanceReason?.let {
      ModelAttendanceReason(
        id = it.attendanceReasonId!!,
        code = it.code,
        description = it.description
      )
    },
    comment = attendance.comment,
    posted = attendance.posted,
    recordedTime = attendance.recordedTime,
    recordedBy = attendance.recordedBy,
    status = attendance.status,
    payAmount = attendance.payAmount,
    bonusAmount = attendance.bonusAmount,
    pieces = attendance.pieces
  )
