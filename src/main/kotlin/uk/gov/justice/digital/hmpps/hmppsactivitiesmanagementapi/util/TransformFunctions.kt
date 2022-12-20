package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityPayCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.PrisonerAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.Priority
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings as PrisonApiCourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PrisonerSchedule as PrisonApiPrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityEligibility as ModelActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance as ModelActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier as ModelActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation as ModelAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason as ModelAttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EligibilityRule as ModelEligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation as ModelInternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents as ModelPrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerWaiting as ModelPrisonerWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrison as ModelRolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent as ModelScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstance as ModelScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Suspension as ModelSuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

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
    attendanceRequired = activity.attendanceRequired,
    summary = activity.summary,
    description = activity.description,
    startDate = activity.startDate,
    endDate = activity.endDate,
    createdTime = activity.createdTime,
    createdBy = activity.createdBy
  )

fun transform(
  activityCreateRequest: ActivityCreateRequest,
  activityCategory: EntityActivityCategory,
  activityTier: EntityActivityTier,
  createdBy: String
) =
  EntityActivity(
    activityId = null,
    prisonCode = activityCreateRequest.prisonCode!!,
    activityCategory = activityCategory,
    activityTier = activityTier,
    attendanceRequired = activityCreateRequest.attendanceRequired,
    summary = activityCreateRequest.summary!!,
    description = activityCreateRequest.description!!,
    startDate = activityCreateRequest.startDate ?: LocalDate.now(),
    endDate = activityCreateRequest.endDate,
    createdTime = LocalDateTime.now(),
    createdBy = createdBy
  )

fun transform(
  activityPayCreateRequests: List<ActivityPayCreateRequest>,
  activityEntity: EntityActivity,
) = activityPayCreateRequests.map { apcr ->
  EntityActivityPay(
    activity = activityEntity,
    incentiveLevel = apcr.incentiveLevel,
    payBand = apcr.payBand,
    rate = apcr.rate,
    pieceRate = apcr.pieceRate,
    pieceRateItems = apcr.pieceRateItems
  )
}

fun transformActivityScheduledInstancesToScheduledEvents(
  bookingId: Long,
  prisonerNumber: String,
  defaultPriority: Int?,
  priorities: List<Priority>?,
  activityScheduledInstances: List<ModelActivityScheduleInstance>,
): List<ModelScheduledEvent> =
  activityScheduledInstances.toModelScheduledEvents(bookingId, prisonerNumber, defaultPriority, priorities)

fun transformToPrisonerScheduledEvents(
  bookingId: Long,
  prisonCode: String,
  prisonerNumber: String,
  dateRange: LocalDateRange,
  eventPriorities: Map<EventType, List<Priority>>,
  appointments: List<PrisonApiScheduledEvent>?,
  courtHearings: PrisonApiCourtHearings?,
  visits: List<PrisonApiScheduledEvent>?,
  activities: List<PrisonApiScheduledEvent>?,
): ModelPrisonerScheduledEvents =
  ModelPrisonerScheduledEvents(
    prisonCode,
    setOf(prisonerNumber),
    dateRange.start,
    dateRange.endInclusive,
    appointments?.prisonApiScheduledEventToScheduledEvents(
      prisonerNumber,
      EventType.APPOINTMENT.name,
      EventType.APPOINTMENT.defaultPriority,
      eventPriorities[EventType.APPOINTMENT]
    ),
    courtHearings?.prisonApiCourtHearingsToScheduledEvents(
      bookingId,
      prisonCode,
      prisonerNumber,
      EventType.COURT_HEARING.name,
      EventType.COURT_HEARING.defaultPriority,
      eventPriorities[EventType.COURT_HEARING]
    ),
    visits?.prisonApiScheduledEventToScheduledEvents(
      prisonerNumber,
      EventType.VISIT.name,
      EventType.VISIT.defaultPriority,
      eventPriorities[EventType.VISIT]
    ),
    activities?.prisonApiScheduledEventToScheduledEvents(
      prisonerNumber,
      EventType.ACTIVITY.name,
      EventType.ACTIVITY.defaultPriority,
      eventPriorities[EventType.ACTIVITY]
    ),
  )

fun transformToPrisonerScheduledEvents(
  prisonCode: String,
  prisonerNumbers: Set<String>,
  date: LocalDate?,
  eventPriorities: Map<EventType, List<Priority>>,
  appointments: List<PrisonApiPrisonerSchedule>?,
  courtEvents: List<PrisonApiPrisonerSchedule>?,
  visits: List<PrisonApiPrisonerSchedule>?,
): ModelPrisonerScheduledEvents =
  ModelPrisonerScheduledEvents(
    prisonCode,
    prisonerNumbers,
    date,
    date,
    appointments?.prisonApiPrisonerScheduleToScheduledEvents(
      prisonCode,
      EventType.APPOINTMENT.name,
      EventType.APPOINTMENT.defaultPriority,
      eventPriorities[EventType.APPOINTMENT]
    ),
    courtEvents?.prisonApiPrisonerScheduleToScheduledEvents(
      prisonCode,
      EventType.COURT_HEARING.name,
      EventType.COURT_HEARING.defaultPriority,
      eventPriorities[EventType.COURT_HEARING]
    ),
    visits?.prisonApiPrisonerScheduleToScheduledEvents(
      prisonCode,
      EventType.VISIT.name,
      EventType.VISIT.defaultPriority,
      eventPriorities[EventType.VISIT]
    ),
    null
  )

fun EntityActivityCategory.toModelActivityCategory() =
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

fun List<EntityActivitySchedule>.toModelSchedules() = map { it.toModelSchedule() }

fun EntityActivitySchedule.toModelSchedule() =
  ModelActivitySchedule(
    id = this.activityScheduleId!!,
    instances = this.instances.toModelScheduledInstances(),
    allocations = this.allocations.toModelAllocations(),
    description = this.description,
    suspensions = this.suspensions.toModelSuspensions(),
    startTime = this.startTime,
    endTime = this.endTime,
    internalLocation = this.toInternalLocation(),
    capacity = this.capacity,
    daysOfWeek = this.getDaysOfWeek().map { day -> day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) },
    activity = this.activity.toModelLite()
  )

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

private fun List<ModelActivityScheduleInstance>.toModelScheduledEvents(
  bookingId: Long?,
  prisonerNumber: String?,
  defaultPriority: Int?,
  priorities: List<Priority>?
) =
  map {
    ModelScheduledEvent(
      prisonCode = it.activitySchedule.activity.prisonCode,
      eventId = it.id,
      bookingId = bookingId,
      locationId = it.activitySchedule.internalLocation?.id?.toLong(),
      location = it.activitySchedule.internalLocation?.description,
      eventClass = "INT_MOV",
      eventStatus = null,
      eventType = "PRISON_ACT",
      eventTypeDesc = "Prison Activities",
      event = it.activitySchedule.activity.summary,
      eventDesc = it.activitySchedule.description,
      details = it.activitySchedule.activity.summary + ": " + it.activitySchedule.description,
      prisonerNumber = prisonerNumber,
      date = it.date,
      startTime = it.startTime,
      endTime = it.endTime,
      priority = priorities?.let { pList -> getPriority(it.activitySchedule.activity.category.code, pList) }
        ?: defaultPriority
    )
  }

private fun getPriority(category: String?, priorities: List<Priority>): Int? =
  priorities.fold(listOf<Priority>()) { acc, next ->
    if (next.eventCategory == null && acc.isEmpty()) listOf(next)
    else when (next.eventCategory) {
      EventCategory.EDUCATION -> if (category?.startsWith("EDU") == true) listOf(next) else acc
      EventCategory.GYM_SPORTS_FITNESS -> if (category?.startsWith("GYM") == true) listOf(next) else acc
      EventCategory.INDUCTION -> if (category == "IND" || category == "INDUC") listOf(next) else acc
      EventCategory.INDUSTRIES -> if (category == "LACO") listOf(next) else acc
      EventCategory.INTERVENTIONS -> if (category == "INTERV") listOf(next) else acc
      EventCategory.LEISURE_SOCIAL -> if (category == "LEI") listOf(next) else acc
      EventCategory.SERVICES -> if (category == "SERV") listOf(next) else acc
      else -> {
        acc
      }
    }
  }.firstOrNull()?.priority

private fun List<PrisonApiScheduledEvent>.prisonApiScheduledEventToScheduledEvents(
  prisonerNumber: String?,
  eventType: String?,
  defaultPriority: Int?,
  priorities: List<Priority>?
) = map {
  ModelScheduledEvent(
    prisonCode = it.agencyId,
    eventId = it.eventId,
    bookingId = it.bookingId,
    locationId = it.eventLocationId,
    location = it.eventLocation,
    eventClass = it.eventClass,
    eventStatus = it.eventStatus,
    eventType = eventType ?: it.eventType,
    eventTypeDesc = it.eventTypeDesc,
    event = it.eventSubType,
    eventDesc = it.eventSubTypeDesc,
    details = it.eventSourceDesc,
    prisonerNumber = prisonerNumber,
    date = it.eventDate,
    startTime = LocalDateTime.parse(it.startTime).toLocalTime(),
    endTime = LocalDateTime.parse(it.endTime).toLocalTime(),
    priority = priorities?.let { pList -> getPriority(it.eventSubType, pList) }
      ?: defaultPriority
  )
}

private fun List<PrisonApiPrisonerSchedule>.prisonApiPrisonerScheduleToScheduledEvents(
  prisonCode: String,
  eventType: String?,
  defaultPriority: Int?,
  priorities: List<Priority>?
) = map { it ->
  ModelScheduledEvent(
    prisonCode = prisonCode,
    eventId = it.eventId,
    bookingId = it.bookingId,
    locationId = it.eventLocationId,
    location = it.eventLocation,
    eventClass = null,
    eventStatus = it.eventStatus,
    eventType = eventType ?: it.eventType,
    eventTypeDesc = null,
    event = it.event,
    eventDesc = it.eventDescription,
    details = it.comment,
    prisonerNumber = it.offenderNo,
    date = LocalDateTime.parse(it.startTime).toLocalDate(),
    startTime = LocalDateTime.parse(it.startTime).toLocalTime(),
    endTime = it.endTime?.let { endTime -> LocalDateTime.parse(endTime).toLocalTime() },
    priority = priorities?.let { pList -> getPriority(it.eventType, pList) }
      ?: defaultPriority
  )
}

private fun PrisonApiCourtHearings.prisonApiCourtHearingsToScheduledEvents(
  bookingId: Long,
  prisonCode: String?,
  prisonerNumber: String?,
  eventType: String?,
  defaultPriority: Int?,
  priorities: List<Priority>?
) = this.hearings?.map {
  ModelScheduledEvent(
    prisonCode = prisonCode,
    eventId = it.id,
    bookingId = bookingId,
    locationId = null,
    location = it.location?.description,
    eventClass = null,
    eventStatus = null,
    eventType = eventType,
    eventTypeDesc = null,
    event = null,
    eventDesc = null,
    details = null,
    prisonerNumber = prisonerNumber,
    date = LocalDateTime.parse(it.dateTime).toLocalDate(),
    startTime = LocalDateTime.parse(it.dateTime).toLocalTime(),
    endTime = null,
    priority = priorities?.let { pList -> getPriority(null, pList) }
      ?: defaultPriority
  )
}

fun List<EntityAllocation>.toModelAllocations() = map {
  ModelAllocation(
    id = it.allocationId!!,
    prisonerNumber = it.prisonerNumber,
    payBand = it.payBand,
    startDate = it.startDate,
    endDate = it.endDate,
    allocatedTime = it.allocatedTime,
    allocatedBy = it.allocatedBy,
    activitySummary = it.activitySummary(),
    scheduleDescription = it.scheduleDescription()
  )
}

fun List<EntityAllocation>.toModelPrisonerAllocations() =
  toModelAllocations().groupBy { it.prisonerNumber }.map { PrisonerAllocations(it.key, it.value) }

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
    status = attendance.status.name,
    payAmount = attendance.payAmount,
    bonusAmount = attendance.bonusAmount,
    pieces = attendance.pieces
  )
