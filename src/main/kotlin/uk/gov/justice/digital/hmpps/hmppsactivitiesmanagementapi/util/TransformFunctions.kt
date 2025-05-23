package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonLocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstanceAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventDescription
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.EarliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.PrisonerAllocations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService.LocationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.EventPriorities
import java.time.DayOfWeek
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity as EntityActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityBasic as EntityActivityBasic
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityEligibility as EntityActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityPay as EntityActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule as EntityActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSuspension as EntitySuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AdvanceAttendance as EntityAdvanceAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AdvanceAttendanceHistory as EntityAdvanceAttendanceHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation as EntityAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance as EntityAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceHistory as EntityAttendanceHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance as EntityScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList as EntityWaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.ActivityCategory as EntityActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventOrganiser as EntityEventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier as EntityEventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand as EntityPrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonRegime as EntityPrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityBasic as ModelActivityBasic
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityEligibility as ModelActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AdvanceAttendance as ModelAdvanceAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AdvanceAttendanceHistory as ModelAdvanceAttendanceHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency as ModelAppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceHistory as ModelAttendanceHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason as ModelAttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EligibilityRule as ModelEligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventReview as ModelEventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventTier as ModelEventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation as ModelInternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand as ModelPrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonRegime as ModelPrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent as ModelScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstance as ModelScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstanceAttendanceSummary as ModelScheduledInstanceAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Suspension as ModelSuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.WaitingListApplication as ModelWaitingListApplication
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

fun transform(activity: EntityActivity) = ModelActivity(
  id = activity.activityId,
  prisonCode = activity.prisonCode,
  category = activity.activityCategory.toModelActivityCategory(),
  tier = activity.activityTier.toModelEventTier(),
  organiser = activity.organiser?.toModelEventOrganiser(),
  eligibilityRules = activity.eligibilityRules().toModelEligibilityRules(),
  schedules = activity.schedules().toModelSchedules(),
  pay = activity.activityPay().toModelActivityPayList(),
  attendanceRequired = activity.attendanceRequired,
  inCell = activity.inCell,
  onWing = activity.onWing,
  offWing = activity.offWing,
  pieceWork = activity.pieceWork,
  outsideWork = activity.outsideWork,
  payPerSession = PayPerSession.valueOf(activity.payPerSession.name),
  summary = activity.summary,
  description = activity.description,
  startDate = activity.startDate,
  endDate = activity.endDate,
  riskLevel = activity.riskLevel,
  createdTime = activity.createdTime,
  createdBy = activity.createdBy,
  updatedTime = activity.updatedTime,
  updatedBy = activity.updatedBy,
  minimumEducationLevel = activity.activityMinimumEducationLevel().toModel(),
  paid = activity.paid,
)

/*
 Transforms a list of activities (view-based) from the local database into a list of
 scheduled events to be returned to API clients.
 */
fun transformPrisonerScheduledActivityToScheduledEvents(
  prisonCode: String,
  priorities: EventPriorities,
  activitiesForPrisoners: List<PrisonerScheduledActivity>,
  prisonLocations: PrisonLocations = emptyMap(),
) = activitiesForPrisoners.map {
  val mayBeInternalLocation = it.internalLocationId?.toLong().let(prisonLocations::get)

  ModelScheduledEvent(
    prisonCode = prisonCode,
    eventSource = "SAA",
    eventType = EventType.ACTIVITY.name,
    scheduledInstanceId = it.scheduledInstanceId,
    // TODO: Add bookingId to allocation and retrieve in the view
    bookingId = it.bookingId,
    internalLocationId = it.internalLocationId?.toLong(),
    internalLocationCode = it.internalLocationCode,
    internalLocationUserDescription = mayBeInternalLocation?.userDescription,
    internalLocationDescription = mayBeInternalLocation?.description ?: it.internalLocationDescription,
    eventId = null,
    appointmentSeriesId = null,
    appointmentId = null,
    appointmentAttendeeId = null,
    oicHearingId = null,
    cancelled = it.cancelled,
    suspended = it.suspended,
    autoSuspended = it.autoSuspended,
    categoryCode = it.activityCategory,
    categoryDescription = it.activityCategory,
    summary = it.scheduleDescription,
    comments = null,
    prisonerNumber = it.prisonerNumber,
    inCell = it.inCell,
    onWing = it.onWing,
    offWing = it.offWing,
    // TODO: Add the outside prison flag to the view
    outsidePrison = false,
    date = it.sessionDate,
    startTime = it.startTime!!,
    endTime = it.endTime,
    priority = priorities.getOrDefault(EventType.ACTIVITY, it.activityCategory),
    appointmentSeriesCancellationStartDate = null,
    appointmentSeriesCancellationStartTime = null,
    appointmentSeriesFrequency = null,
    paidActivity = it.paidActivity,
    issuePayment = it.issuePayment,
    attendanceStatus = it.attendanceStatus?.name,
    attendanceReasonCode = it.attendanceReasonCode?.name,
  )
}

/*
 Transforms a list of appointment instances from the local database into a list of
 scheduled events to be returned to API clients.
 */
fun transformAppointmentInstanceToScheduledEvents(
  prisonCode: String,
  priorities: EventPriorities,
  referenceCodesForAppointmentsMap: Map<String, ReferenceCode>,
  locationsForAppointmentsMap: Map<Long, LocationDetails>,
  appointments: List<AppointmentInstance>,
) = appointments.map {
  val locationCode: String
  val locationDescription: String

  when {
    it.inCell -> {
      locationCode = "In cell"
      locationDescription = "In cell"
    } else -> {
      locationCode = locationsForAppointmentsMap[it.internalLocationId]?.code ?: "No information available"
      locationDescription = locationsForAppointmentsMap[it.internalLocationId]?.description ?: "No information available"
    }
  }

  ModelScheduledEvent(
    prisonCode = prisonCode,
    eventSource = "SAA",
    eventType = EventType.APPOINTMENT.name,
    scheduledInstanceId = null,
    bookingId = it.bookingId,
    internalLocationId = it.internalLocationId,
    internalLocationCode = locationCode,
    internalLocationUserDescription = locationDescription,
    internalLocationDescription = locationDescription,
    eventId = null,
    appointmentSeriesId = it.appointmentSeriesId,
    appointmentId = it.appointmentId,
    appointmentAttendeeId = it.appointmentAttendeeId,
    oicHearingId = null,
    cancelled = it.isCancelled,
    suspended = false,
    categoryCode = it.categoryCode,
    categoryDescription = referenceCodesForAppointmentsMap[it.categoryCode].toAppointmentCategorySummary(it.categoryCode).description,
    summary = referenceCodesForAppointmentsMap[it.categoryCode].toAppointmentName(
      it.categoryCode,
      it.customName,
    ),
    comments = it.extraInformation,
    prisonerNumber = it.prisonerNumber,
    inCell = it.inCell,
    onWing = false,
    outsidePrison = false,
    date = it.appointmentDate,
    startTime = it.startTime,
    endTime = it.endTime,
    priority = priorities.getOrDefault(EventType.APPOINTMENT),
    appointmentSeriesCancellationStartDate = it.seriesCancellationStartDate,
    appointmentSeriesCancellationStartTime = it.seriesCancellationStartTime,
    appointmentSeriesFrequency = it.seriesFrequency?.let { it1 -> ModelAppointmentFrequency.valueOf(it1.name) },
    paidActivity = null,
    issuePayment = null,
    attendanceStatus = null,
    attendanceReasonCode = null,
  )
}

fun EntityActivityCategory.toModelActivityCategory() = ModelActivityCategory(
  this.activityCategoryId,
  this.code,
  this.name,
  this.description,
)

fun EntityEventTier.toModelEventTier() = ModelEventTier(
  id = this.eventTierId,
  code = this.code,
  description = this.description,
)

fun EntityEventOrganiser.toModelEventOrganiser() = EventOrganiser(
  id = this.eventOrganiserId,
  code = this.code,
  description = this.description,
)

private fun List<EntityActivityEligibility>.toModelEligibilityRules() = map {
  ModelActivityEligibility(
    it.activityEligibilityId,
    it.eligibilityRule.let { er -> ModelEligibilityRule(er.eligibilityRuleId, er.code, er.description) },
  )
}

fun transform(scheduleEntities: List<EntityActivitySchedule>) = scheduleEntities.toModelSchedules()

fun List<EntityActivitySchedule>.toModelSchedules() = map { it.toModelSchedule() }

fun EntityActivitySchedule.toModelSchedule() = ModelActivitySchedule(
  id = this.activityScheduleId,
  instances = this.instances().toModelScheduledInstances(),
  allocations = this.allocations().toModelAllocations(),
  description = this.description,
  suspensions = this.suspensions.toModelSuspensions(),
  internalLocation = this.toInternalLocation(),
  capacity = this.capacity,
  activity = this.activity.toModelLite(),
  scheduleWeeks = this.scheduleWeeks,
  slots = this.toModelActivityScheduleSlots(),
  startDate = this.startDate,
  endDate = this.endDate,
  runsOnBankHoliday = this.runsOnBankHoliday,
  updatedTime = this.updatedTime,
  updatedBy = this.updatedBy,
  usePrisonRegimeTime = this.usePrisonRegimeTime,
)

private fun List<EntityScheduledInstance>.toModelScheduledInstances() = map {
  ModelScheduledInstance(
    id = it.scheduledInstanceId,
    date = it.sessionDate,
    startTime = it.startTime,
    endTime = it.endTime,
    timeSlot = it.timeSlot,
    cancelled = it.cancelled,
    cancelledTime = it.cancelledTime,
    cancelledBy = it.cancelledBy,
    attendances = it.attendances.map { attendance -> transform(attendance, null) },
  )
}

fun List<EntityAllocation>.toModelAllocations() = map { it.toModel() }

fun List<EntityAllocation>.toModelPrisonerAllocations() = toModelAllocations().groupBy { it.prisonerNumber }.map { PrisonerAllocations(it.key, it.value) }

private fun List<EntitySuspension>.toModelSuspensions() = map {
  ModelSuspension(
    suspendedFrom = it.suspendedFrom,
    suspendedUntil = it.suspendedUntil,
  )
}

private fun List<EntityActivityPay>.toModelActivityPayList() = map {
  ModelActivityPay(
    id = it.activityPayId,
    incentiveNomisCode = it.incentiveNomisCode,
    incentiveLevel = it.incentiveLevel,
    prisonPayBand = it.payBand.toModelPrisonPayBand(),
    rate = it.rate,
    pieceRate = it.pieceRate,
    pieceRateItems = it.pieceRateItems,
    startDate = it.startDate,
  )
}

private fun EntityActivitySchedule.toInternalLocation() = internalLocationId?.let {
  ModelInternalLocation(
    id = internalLocationId!!,
    code = internalLocationCode!!,
    description = internalLocationDescription!!,
    dpsLocationId = dpsLocationId,
  )
}

fun transform(attendance: EntityAttendance, caseNotesApiClient: CaseNotesApiClient?, includeHistory: Boolean = false): ModelAttendance {
  var history: List<AttendanceHistory>? = null

  if (includeHistory) {
    history = attendance.history()
      .sortedWith(compareBy { attendance.recordedTime })
      .reversed()
      .map { attendanceHistory: EntityAttendanceHistory ->
        transform(
          attendanceHistory,
          attendance.prisonerNumber,
          caseNotesApiClient,
        )
      }
  }

  return ModelAttendance(
    id = attendance.attendanceId,
    scheduleInstanceId = attendance.scheduledInstance.scheduledInstanceId,
    prisonerNumber = attendance.prisonerNumber,
    attendanceReason = attendance.attendanceReason?.let {
      ModelAttendanceReason(
        id = it.attendanceReasonId,
        code = it.code.toString(),
        description = it.description,
        attended = it.attended,
        capturePay = it.capturePay,
        captureMoreDetail = it.captureMoreDetail,
        captureCaseNote = it.captureCaseNote,
        captureIncentiveLevelWarning = it.captureIncentiveLevelWarning,
        captureOtherText = it.captureOtherText,
        displayInAbsence = it.displayInAbsence,
        displaySequence = it.displaySequence,
        notes = it.notes,
      )
    },
    comment = attendance.comment,
    recordedTime = attendance.recordedTime,
    recordedBy = attendance.recordedBy,
    status = attendance.status().name,
    payAmount = attendance.payAmount,
    bonusAmount = attendance.bonusAmount,
    pieces = attendance.pieces,
    issuePayment = attendance.issuePayment,
    incentiveLevelWarningIssued = attendance.incentiveLevelWarningIssued,
    caseNoteText = attendance.caseNoteId?.let {
      caseNotesApiClient?.getCaseNote(
        attendance.prisonerNumber,
        attendance.caseNoteId!!,
      )?.text
    },
    otherAbsenceReason = attendance.otherAbsenceReason,
    attendanceHistory = history,
    editable = attendance.editable(),
    payable = attendance.isPayable(),
  )
}

fun transform(attendance: EntityAdvanceAttendance, includeHistory: Boolean = false, payAmount: Int? = null): ModelAdvanceAttendance {
  var history: List<ModelAdvanceAttendanceHistory>? = null

  if (includeHistory) {
    history = attendance.history()
      .sortedWith(compareBy { attendance.recordedTime })
      .reversed()
      .map { attendanceHistory: EntityAdvanceAttendanceHistory ->
        transform(attendanceHistory)
      }
  }

  return ModelAdvanceAttendance(
    id = attendance.advanceAttendanceId,
    scheduleInstanceId = attendance.scheduledInstance.scheduledInstanceId,
    prisonerNumber = attendance.prisonerNumber,
    issuePayment = attendance.issuePayment,
    payAmount = payAmount,
    recordedTime = attendance.recordedTime,
    recordedBy = attendance.recordedBy,
    attendanceHistory = history,
  )
}

fun transform(
  attendanceHistory: EntityAttendanceHistory,
  prisonerNumber: String,
  caseNotesApiClient: CaseNotesApiClient?,
): ModelAttendanceHistory = ModelAttendanceHistory(
  id = attendanceHistory.attendanceHistoryId,
  attendanceReason = attendanceHistory.attendanceReason?.let {
    ModelAttendanceReason(
      id = it.attendanceReasonId,
      code = it.code.toString(),
      description = it.description,
      attended = it.attended,
      capturePay = it.capturePay,
      captureMoreDetail = it.captureMoreDetail,
      captureCaseNote = it.captureCaseNote,
      captureIncentiveLevelWarning = it.captureIncentiveLevelWarning,
      captureOtherText = it.captureOtherText,
      displayInAbsence = it.displayInAbsence,
      displaySequence = it.displaySequence,
      notes = it.notes,
    )
  },
  comment = attendanceHistory.comment,
  recordedTime = attendanceHistory.recordedTime,
  recordedBy = attendanceHistory.recordedBy,
  issuePayment = attendanceHistory.issuePayment,
  incentiveLevelWarningIssued = attendanceHistory.incentiveLevelWarningIssued,
  otherAbsenceReason = attendanceHistory.otherAbsenceReason,
  caseNoteText = attendanceHistory.caseNoteId?.let {
    caseNotesApiClient?.getCaseNote(
      prisonerNumber,
      attendanceHistory.caseNoteId!!,
    )?.text
  },
)

fun transform(
  attendanceHistory: EntityAdvanceAttendanceHistory,
): ModelAdvanceAttendanceHistory = ModelAdvanceAttendanceHistory(
  id = attendanceHistory.advanceAttendanceHistoryId,
  issuePayment = attendanceHistory.issuePayment,
  recordedTime = attendanceHistory.recordedTime,
  recordedBy = attendanceHistory.recordedBy,
)

fun EntityPrisonPayBand.toModelPrisonPayBand() = ModelPrisonPayBand(
  id = this.prisonPayBandId,
  alias = this.payBandAlias,
  description = this.payBandDescription,
  displaySequence = this.displaySequence,
  nomisPayBand = this.nomisPayBand,
  prisonCode = this.prisonCode,
  createdTime = this.createdTime,
  createdBy = this.createdBy,
  updatedTime = this.updatedTime,
  updatedBy = this.updatedBy,
)

fun transform(prisonRegime: EntityPrisonRegime, dayOfWeek: DayOfWeek) = ModelPrisonRegime(
  id = prisonRegime.prisonRegimeId,
  prisonCode = prisonRegime.prisonCode,
  amStart = prisonRegime.amStart,
  amFinish = prisonRegime.amFinish,
  pmStart = prisonRegime.pmStart,
  pmFinish = prisonRegime.pmFinish,
  edStart = prisonRegime.edStart,
  edFinish = prisonRegime.edFinish,
  dayOfWeek = dayOfWeek,
)

fun transform(entityEventReview: EventReview) = ModelEventReview(
  eventReviewId = entityEventReview.eventReviewId,
  serviceIdentifier = entityEventReview.serviceIdentifier,
  eventType = entityEventReview.eventType,
  eventTime = entityEventReview.eventTime,
  prisonCode = entityEventReview.prisonCode,
  prisonerNumber = entityEventReview.prisonerNumber,
  bookingId = entityEventReview.bookingId,
  eventData = entityEventReview.eventData,
  acknowledgedTime = entityEventReview.acknowledgedTime,
  acknowledgedBy = entityEventReview.acknowledgedBy,
  eventDescription = enumValues<EventDescription>().firstOrNull { it.name == entityEventReview.eventDescription?.name },
)

fun transform(activityBasic: EntityActivityBasic) = ModelActivityBasic(
  activityId = activityBasic.activityId,
  prisonCode = activityBasic.prisonCode,
  activityScheduleId = activityBasic.activityScheduleId,
  startDate = activityBasic.startDate,
  endDate = activityBasic.endDate,
  summary = activityBasic.summary,
  categoryId = activityBasic.categoryId,
  categoryCode = activityBasic.categoryCode,
  categoryName = activityBasic.categoryName,
)

fun ScheduledInstanceAttendanceSummary.toModel() = (
  ModelScheduledInstanceAttendanceSummary(
    scheduledInstanceId = this.scheduledInstanceId,
    activityId = this.activityId,
    activityScheduleId = this.activityScheduleId,
    summary = this.summary,
    categoryId = this.activityCategoryId,
    sessionDate = this.sessionDate,
    startTime = this.startTime,
    endTime = this.endTime,
    inCell = this.inCell,
    onWing = this.onWing,
    offWing = this.offWing,
    attendanceRequired = this.attendanceRequired,
    internalLocation = this.internalLocationId?.let {
      InternalLocation(
        this.internalLocationId.toInt(),
        this.internalLocationCode!!,
        this.internalLocationDescription!!,
      )
    },
    cancelled = this.cancelled,
    timeSlot = this.timeSlot,
    attendanceSummary = ModelScheduledInstanceAttendanceSummary.AttendanceSummaryDetails(
      allocations = this.allocations,
      attendees = this.attendees,
      notRecorded = this.notRecorded,
      attended = this.attended,
      absences = this.absences,
      paid = this.paid,
    ),
  )
  )

fun EntityWaitingList.toModel(earliestReleaseDate: EarliestReleaseDate, nonAssociations: Boolean? = false) = ModelWaitingListApplication(
  id = waitingListId,
  prisonCode = prisonCode,
  activityId = activitySchedule.activity.activityId,
  scheduleId = activitySchedule.activityScheduleId,
  allocationId = allocation?.allocationId,
  prisonerNumber = prisonerNumber,
  bookingId = bookingId,
  status = status,
  declinedReason = declinedReason,
  requestedDate = applicationDate,
  requestedBy = requestedBy,
  comments = comments,
  createdBy = createdBy,
  creationTime = creationTime,
  updatedTime = updatedTime,
  updatedBy = updatedBy,
  statusUpdatedTime = statusUpdatedTime,
  earliestReleaseDate = earliestReleaseDate,
  nonAssociations = nonAssociations,
)
