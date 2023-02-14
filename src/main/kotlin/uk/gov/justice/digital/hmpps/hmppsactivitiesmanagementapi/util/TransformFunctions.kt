package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityMinimumEducationLevel as EntityActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule as EntityActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSlot as EntityActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSuspension as EntitySuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier as EntityActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation as EntityAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance as EntityAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonPayBand as EntityPrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonRegime as EntityPrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerWaiting as EntityPrisonerWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison as EntityRolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance as EntityScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityEligibility as ModelActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel as ModelActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleInstance as ModelActivityScheduleInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot as ModelActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier as ModelActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason as ModelAttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EligibilityRule as ModelEligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation as ModelInternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand as ModelPrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonRegime as ModelPrisonRegime
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
    id = activity.activityId,
    prisonCode = activity.prisonCode,
    category = activity.activityCategory.toModelActivityCategory(),
    tier = activity.activityTier?.toModelActivityTier(),
    eligibilityRules = activity.eligibilityRules().toModelEligibilityRules(),
    schedules = activity.schedules().toModelSchedules(),
    waitingList = activity.waitingList.toModelWaitingList(),
    pay = activity.activityPay().toModelActivityPayList(),
    attendanceRequired = activity.attendanceRequired,
    inCell = activity.inCell,
    pieceWork = activity.pieceWork,
    outsideWork = activity.outsideWork,
    payPerSession = PayPerSession.valueOf(activity.payPerSession.name),
    summary = activity.summary,
    description = activity.description,
    startDate = activity.startDate,
    endDate = activity.endDate,
    riskLevel = activity.riskLevel,
    minimumIncentiveLevel = activity.minimumIncentiveLevel,
    createdTime = activity.createdTime,
    createdBy = activity.createdBy,
    minimumEducationLevel = activity.activityMinimumEducationLevel().toModelActivityMinimumEducationLevelList()
  )

fun transformPrisonerScheduledActivityToScheduledEvents(
  prisonCode: String,
  defaultPriority: Int?,
  priorities: List<Priority>?,
  activitiesForPrisoners: List<PrisonerScheduledActivity>,
): List<ModelScheduledEvent> =
  activitiesForPrisoners.toModelScheduledEvents(prisonCode, defaultPriority, priorities)

fun List<PrisonerScheduledActivity>.toModelScheduledEvents(
  prisonCode: String,
  defaultPriority: Int?,
  priorities: List<Priority>?,
): List<ModelScheduledEvent> =
  map {
    ModelScheduledEvent(
      prisonCode = prisonCode,
      eventId = it.scheduledInstanceId,
      bookingId = it.bookingId.toLong(), // Change allocation to include bookingId
      locationId = it.internalLocationId?.toLong(),
      location = it.internalLocationDescription,
      eventClass = "INT_MOV",
      eventStatus = null, // Can determine from attendance later
      eventType = "PRISON_ACT",
      eventTypeDesc = it.activityCategory,
      event = it.activitySummary,
      eventDesc = it.scheduleDescription,
      details = it.activitySummary + ": " + it.scheduleDescription,
      prisonerNumber = it.prisonerNumber,
      date = it.sessionDate,
      startTime = it.startTime!!,
      endTime = it.endTime,
      priority = priorities?.let { pList -> getPriority(it.activityCategory, pList) } ?: defaultPriority
    )
  }

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
  activities: List<PrisonApiPrisonerSchedule>?,
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
      eventPriorities[EventType.APPOINTMENT],
    ),
    courtEvents?.prisonApiPrisonerScheduleToScheduledEvents(
      prisonCode,
      EventType.COURT_HEARING.name,
      EventType.COURT_HEARING.defaultPriority,
      eventPriorities[EventType.COURT_HEARING],
    ),
    visits?.prisonApiPrisonerScheduleToScheduledEvents(
      prisonCode,
      EventType.VISIT.name,
      EventType.VISIT.defaultPriority,
      eventPriorities[EventType.VISIT],
    ),
    activities?.prisonApiPrisonerScheduleToScheduledEvents(
      prisonCode,
      EventType.ACTIVITY.name,
      EventType.ACTIVITY.defaultPriority,
      eventPriorities[EventType.ACTIVITY],
    )
  )

fun transformToPrisonerScheduledEvents(
  prisonCode: String,
  dateRange: LocalDateRange,
  eventPriorities: Map<EventType, List<Priority>>,
  appointments: List<PrisonApiPrisonerSchedule>?,
  courtEvents: List<PrisonApiPrisonerSchedule>?,
  visits: List<PrisonApiPrisonerSchedule>?,
  activities: List<PrisonApiPrisonerSchedule>?,
): ModelPrisonerScheduledEvents =
  ModelPrisonerScheduledEvents(
    prisonCode,
    emptySet(),
    dateRange.start,
    dateRange.endInclusive,
    appointments?.prisonApiPrisonerScheduleToScheduledEvents(
      prisonCode,
      EventType.APPOINTMENT.name,
      EventType.APPOINTMENT.defaultPriority,
      eventPriorities[EventType.APPOINTMENT],
    ),
    courtEvents?.prisonApiPrisonerScheduleToScheduledEvents(
      prisonCode,
      EventType.COURT_HEARING.name,
      EventType.COURT_HEARING.defaultPriority,
      eventPriorities[EventType.COURT_HEARING],
    ),
    visits?.prisonApiPrisonerScheduleToScheduledEvents(
      prisonCode,
      EventType.VISIT.name,
      EventType.VISIT.defaultPriority,
      eventPriorities[EventType.VISIT],
    ),
    activities?.prisonApiPrisonerScheduleToScheduledEvents(
      prisonCode,
      EventType.ACTIVITY.name,
      EventType.ACTIVITY.defaultPriority,
      eventPriorities[EventType.ACTIVITY]
    ),
  )

fun EntityActivityCategory.toModelActivityCategory() =
  ModelActivityCategory(
    this.activityCategoryId,
    this.code,
    this.name,
    this.description
  )

private fun EntityActivityTier.toModelActivityTier() =
  ModelActivityTier(
    id = this.activityTierId,
    code = this.code,
    description = this.description,
  )

private fun List<EntityActivityEligibility>.toModelEligibilityRules() = map {
  ModelActivityEligibility(
    it.activityEligibilityId,
    it.eligibilityRule.let { er -> ModelEligibilityRule(er.eligibilityRuleId, er.code, er.description) }
  )
}

fun transform(scheduleEntities: List<EntityActivitySchedule>) = scheduleEntities.toModelSchedules()

fun transformFilteredInstances(scheduleAndInstances: Map<EntityActivitySchedule, List<EntityScheduledInstance>>) =
  scheduleAndInstances.map {
    ModelActivitySchedule(
      id = it.key.activityScheduleId,
      instances = it.value.toModelScheduledInstances(),
      allocations = it.key.allocations().toModelAllocations(),
      description = it.key.description,
      suspensions = it.key.suspensions.toModelSuspensions(),
      internalLocation = it.key.toInternalLocation(),
      capacity = it.key.capacity,
      activity = it.key.activity.toModelLite(),
      slots = it.key.slots().toModelActivityScheduleSlots(),
      startDate = it.key.startDate,
      endDate = it.key.endDate
    )
  }

fun List<EntityActivitySchedule>.toModelSchedules() = map { it.toModelSchedule() }

fun EntityActivitySchedule.toModelSchedule() =
  ModelActivitySchedule(
    id = this.activityScheduleId,
    instances = this.instances().toModelScheduledInstances(),
    allocations = this.allocations().toModelAllocations(),
    description = this.description,
    suspensions = this.suspensions.toModelSuspensions(),
    internalLocation = this.toInternalLocation(),
    capacity = this.capacity,
    activity = this.activity.toModelLite(),
    slots = this.slots().toModelActivityScheduleSlots(),
    startDate = this.startDate,
    endDate = this.endDate
  )

private fun List<EntityPrisonerWaiting>.toModelWaitingList() = map {
  ModelPrisonerWaiting(
    id = it.prisonerWaitingId,
    prisonerNumber = it.prisonerNumber,
    priority = it.priority,
    createdTime = it.createdTime,
    createdBy = it.createdBy,
  )
}

private fun List<EntityActivityScheduleSlot>.toModelActivityScheduleSlots() = map {
  ModelActivityScheduleSlot(
    id = it.activityScheduleSlotId,
    startTime = it.startTime,
    endTime = it.endTime,
    daysOfWeek = it.getDaysOfWeek().map { day -> day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) },
  )
}

private fun List<EntityScheduledInstance>.toModelScheduledInstances() = map {
  ModelScheduledInstance(
    id = it.scheduledInstanceId,
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
    endTime = it.endTime?.let { endTime -> LocalDateTime.parse(endTime).toLocalTime() },
    priority = priorities?.let { pList -> getPriority(it.eventSubType, pList) }
      ?: defaultPriority
  )
}

private fun List<PrisonApiPrisonerSchedule>.prisonApiPrisonerScheduleToScheduledEvents(
  prisonCode: String,
  eventType: String?,
  defaultPriority: Int?,
  priorities: List<Priority>?
) = map {
  ModelScheduledEvent(
    prisonCode = prisonCode,
    eventId = it.eventId,
    bookingId = it.bookingId,
    locationId = it.locationId,
    location = it.eventLocation ?: "External", // Don't show the real court location
    eventClass = it.event,
    eventStatus = it.eventStatus,
    eventType = eventType ?: it.eventType,
    eventTypeDesc = eventType ?: it.eventType,
    event = it.event,
    eventDesc = it.eventDescription,
    details = it.comment ?: it.eventDescription,
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

fun List<EntityAllocation>.toModelAllocations() = map { it.toModel() }

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
    id = it.activityPayId,
    incentiveLevel = it.incentiveLevel,
    prisonPayBand = it.payBand.toModelPrisonPayBand(),
    rate = it.rate,
    pieceRate = it.pieceRate,
    pieceRateItems = it.pieceRateItems
  )
}

private fun List<EntityActivityMinimumEducationLevel>.toModelActivityMinimumEducationLevelList() = map {
  ModelActivityMinimumEducationLevel(
    id = it.activityMinimumEducationLevelId,
    educationLevelCode = it.educationLevelCode,
    educationLevelDescription = it.educationLevelDescription
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
  id = prison.rolloutPrisonId,
  code = prison.code,
  description = prison.description,
  active = prison.active,
  rolloutDate = prison.rolloutDate
)

fun transform(attendance: EntityAttendance): ModelAttendance =
  ModelAttendance(
    id = attendance.attendanceId,
    prisonerNumber = attendance.prisonerNumber,
    attendanceReason = attendance.attendanceReason?.let {
      ModelAttendanceReason(
        id = it.attendanceReasonId,
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

fun EntityPrisonPayBand.toModelPrisonPayBand() =
  ModelPrisonPayBand(
    id = this.prisonPayBandId,
    alias = this.payBandAlias,
    description = this.payBandDescription,
    displaySequence = this.displaySequence,
    nomisPayBand = this.nomisPayBand,
    prisonCode = this.prisonCode
  )

fun transform(prisonRegime: EntityPrisonRegime) = ModelPrisonRegime(
  id = prisonRegime.prisonRegimeId,
  prisonCode = prisonRegime.prisonCode,
  amStart = prisonRegime.amStart,
  amFinish = prisonRegime.amFinish,
  pmStart = prisonRegime.pmStart,
  pmFinish = prisonRegime.pmFinish,
  edStart = prisonRegime.edStart,
  edFinish = prisonRegime.edFinish
)
