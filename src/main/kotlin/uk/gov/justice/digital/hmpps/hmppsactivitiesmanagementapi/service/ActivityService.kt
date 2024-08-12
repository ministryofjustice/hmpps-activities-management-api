package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.containsAny
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SlotTimes
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTierType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModelLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMinimumEducationLevelCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityPayCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivitySummaryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EligibilityRuleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventOrganiserRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.findByCodeOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.BankHolidayService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.ACTIVITY_NAME_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_ORGANISER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIER_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.PRISON_CODE_PROPERTY_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.activityMetricsMap
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.checkCaseloadAccess
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toActivityBasicList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityBasic as ModelActivityBasic

typealias AllocationIds = Set<Long>

@Service
@Transactional(readOnly = true)
class ActivityService(
  @Value("\${migrate.experimental-mode}") val experimentalMode: Boolean,
  private val activityRepository: ActivityRepository,
  private val activitySummaryRepository: ActivitySummaryRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
  private val eventTierRepository: EventTierRepository,
  private val eventOrganiserRepository: EventOrganiserRepository,
  private val eligibilityRuleRepository: EligibilityRuleRepository,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val prisonPayBandRepository: PrisonPayBandRepository,
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonRegimeService: PrisonRegimeService,
  private val bankHolidayService: BankHolidayService,
  private val telemetryClient: TelemetryClient,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
  @Value("\${online.create-scheduled-instances.days-in-advance}") private val daysInAdvance: Long = 14L,
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getActivityByIdWithFilters(activityId: Long, earliestSessionDate: LocalDate?): ModelActivity {
    // TODO: Caseload check
    val earliestSession = earliestSessionDate ?: LocalDate.now().minusMonths(1)
    val activity = activityRepository.getActivityByIdWithFilters(activityId, earliestSession)
      ?: throw (EntityNotFoundException("Activity $activityId not found"))
    return transform(activity)
  }

  fun getActivityById(activityId: Long): ModelActivity {
    val activity = activityRepository.findById(activityId)
      .orElseThrow { EntityNotFoundException("Activity $activityId not found") }
    checkCaseloadAccess(activity.prisonCode)
    return transform(activity)
  }

  fun getActivityBasicById(activityId: Long): ModelActivityBasic {
    // TODO: Caseload check
    val activityBasic = activityRepository.getActivityBasicById(activityId)
      ?: throw EntityNotFoundException("Activity $activityId not found.")
    return transform(activityBasic)
  }

  fun getActivityBasicByPrisonCode(prisonCode: String): List<ModelActivityBasic> {
    return activityRepository.getActivityBasicByPrisonCode(prisonCode).toActivityBasicList()
  }

  fun getActivitiesByCategoryInPrison(
    prisonCode: String,
    categoryId: Long,
  ) =
    activityCategoryRepository.findOrThrowNotFound(categoryId).let {
      activityRepository.getAllByPrisonCodeAndActivityCategory(prisonCode, it).toModelLite()
    }

  fun getActivitiesInPrison(
    prisonCode: String,
    excludeArchived: Boolean,
  ) = activitySummaryRepository.findAllByPrisonCode(prisonCode)
    .filter { !excludeArchived || it.activityState != ActivityState.ARCHIVED }.toModel()

  fun getSchedulesForActivity(activityId: Long): List<ActivityScheduleLite> {
    val activity = activityRepository.findById(activityId)
      .orElseThrow { EntityNotFoundException("Activity $activityId not found") }
    return activityScheduleRepository.getAllByActivity(activity).toModelLite()
  }

  private fun failDuplicateActivity(prisonCode: String, summary: String) {
    val duplicateActivity = activityRepository.existingLiveActivity(prisonCode, summary, LocalDate.now())

    if (duplicateActivity) {
      throw IllegalArgumentException("Change the activity name. There is already an activity called '$summary'")
    }
  }

  @Transactional
  fun createActivity(request: ActivityCreateRequest, createdBy: String): ModelActivity {
    checkCaseloadAccess(request.prisonCode!!)

    require(request.startDate!! > LocalDate.now()) { "Activity start date must be in the future" }
    require((request.locationId != null) xor request.offWing xor request.onWing xor request.inCell) { "Activity location can only be maximum one of offWing, onWing, inCell, or a specified location" }
    if (request.paid.not() && request.pay.isNotEmpty()) throw IllegalArgumentException("Unpaid activity cannot have pay rates associated with it")
    if (request.paid && request.pay.isEmpty()) throw IllegalArgumentException("Paid activity must have at least one pay rate associated with it")

    return transactionHandler.newSpringTransaction {
      val category = activityCategoryRepository.findOrThrowIllegalArgument(request.categoryId!!)
      val tier = eventTierRepository.findByCodeOrThrowIllegalArgument(request.tierCode!!)

      if (category.isNotInWork() && !tier.isFoundation()) throw IllegalArgumentException("Activity category NOT IN WORK must be a Foundation Tier")

      val organiser = request.organiserCode?.let { eventOrganiserRepository.findByCodeOrThrowIllegalArgument(it) }
      val eligibilityRules = request.eligibilityRuleIds.map { eligibilityRuleRepository.findOrThrowIllegalArgument(it) }
      val prisonPayBands = prisonPayBandRepository.findByPrisonCode(request.prisonCode)
        .associateBy { it.prisonPayBandId }
        .ifEmpty { throw IllegalArgumentException("No pay bands found for prison '${request.prisonCode}") }
      failDuplicateActivity(request.prisonCode, request.summary!!)
      checkEducationLevels(request.minimumEducationLevel)

      val activity = Activity(
        prisonCode = request.prisonCode,
        activityCategory = category,
        activityTier = tier,
        attendanceRequired = request.attendanceRequired,
        summary = request.summary,
        description = request.description,
        inCell = request.inCell,
        onWing = request.onWing,
        offWing = request.offWing,
        startDate = request.startDate,
        riskLevel = request.riskLevel!!,
        createdTime = LocalDateTime.now(),
        createdBy = createdBy,
        isPaid = request.paid,
      ).apply {
        this.organiser = organiser
        endDate = request.endDate
        eligibilityRules.forEach { this.addEligibilityRule(it) }
        request.pay.forEach {
          this.addPay(
            incentiveNomisCode = it.incentiveNomisCode!!,
            incentiveLevel = it.incentiveLevel!!,
            payBand = prisonPayBands[it.payBandId]
              ?: throw IllegalArgumentException("Pay band not found for prison '${request.prisonCode}'"),
            rate = it.rate,
            pieceRate = it.pieceRate,
            pieceRateItems = it.pieceRateItems,
            startDate = it.startDate,
          )
        }
        request.minimumEducationLevel.forEach {
          this.addMinimumEducationLevel(
            educationLevelCode = it.educationLevelCode!!,
            educationLevelDescription = it.educationLevelDescription!!,
            studyAreaCode = it.studyAreaCode!!,
            studyAreaDescription = it.studyAreaDescription!!,
          )
        }
      }

      activity.let {
        val scheduleLocation = if (request.inCell || request.onWing || request.offWing) null else getLocationForSchedule(it, request.locationId!!)
        val usesPrisonRegimeTime = request.slots?.all { s -> s.customStartTime == null && s.customEndTime == null } == true
        activity.addSchedule(
          description = request.description!!,
          internalLocation = scheduleLocation,
          capacity = request.capacity!!,
          startDate = request.startDate,
          endDate = request.endDate,
          runsOnBankHoliday = request.runsOnBankHoliday,
          scheduleWeeks = request.scheduleWeeks,
          usesPrisonRegimeTime = request.slots == null || usesPrisonRegimeTime,
        ).let { schedule ->
          schedule.addSlots(request.slots!!)
          schedule.addInstances()

          val activityModel = transform(activityRepository.saveAndFlush(activity))

          publishCreateTelemetryEvent(activityModel)

          activityModel
        }
      }
    }.also { outboundEventsService.send(OutboundEvent.ACTIVITY_SCHEDULE_CREATED, it.schedules.single().id) }
  }

  private fun ModelActivity.toTelemetryPropertiesMap() =
    mutableMapOf(
      PRISON_CODE_PROPERTY_KEY to this.prisonCode,
      ACTIVITY_NAME_PROPERTY_KEY to this.summary,
    ).also { propsMap ->
      this.tier?.let { propsMap[EVENT_TIER_PROPERTY_KEY] = it.description }
      this.organiser?.let { propsMap[EVENT_ORGANISER_PROPERTY_KEY] = it.description }
    }

  private fun publishCreateTelemetryEvent(activity: ModelActivity) =
    telemetryClient.trackEvent(TelemetryEvent.ACTIVITY_CREATED.value, activity.toTelemetryPropertiesMap(), activityMetricsMap())

  private fun publishUpdateTelemetryEvent(activity: ModelActivity) =
    telemetryClient.trackEvent(TelemetryEvent.ACTIVITY_EDITED.value, activity.toTelemetryPropertiesMap(), activityMetricsMap())

  private fun checkEducationLevels(minimumEducationLevels: List<ActivityMinimumEducationLevelCreateRequest>) {
    minimumEducationLevels.forEach {
      prisonApiClient.getEducationLevel(it.educationLevelCode!!).block()!!.also { educationLevel ->
        require(educationLevel.isActive()) { "The education level code '${educationLevel.code}' is not active in NOMIS" }
        require(it.educationLevelDescription!! == educationLevel.description) {
          "The education level description '${it.educationLevelDescription}' does not match the NOMIS education level '${educationLevel.description}'"
        }
      }

      prisonApiClient.getStudyArea(it.studyAreaCode!!).block()!!.also { studyArea ->
        require(studyArea.isActive()) { "The study area code '${studyArea.code}' is not active in NOMIS" }
        require(it.studyAreaDescription!! == studyArea.description) {
          "The study area description '${it.studyAreaDescription}' does not match the NOMIS study area '${studyArea.description}'"
        }
      }
    }
  }

  private fun ActivitySchedule.addSlots(slots: List<Slot>) {
    slots.forEach { slot ->
      if (slot.customStartTime != null && slot.customEndTime != null) {
        this.addCustomSlot(slot = slot)
      } else {
        prisonRegimeService.getSlotTimesForDaysOfWeek(
          prisonCode = activity.prisonCode,
          daysOfWeek = slot.daysOfWeek,
          acrossRegimes = true,
        )?.filter { it.key.containsAny(slot.daysOfWeek) }?.forEach { timeSlot ->
          this.addRegimeSlot(
            slot = slot,
            timeSlot = timeSlot.value,
            daysOfWeekToApply = timeSlot.key.filter { slot.daysOfWeek.contains(it) }.toSet(),
          )
        } ?: throw ValidationException("unable to add $slot as no applicable timeslots found")
      }
    }
  }

  private fun ActivitySchedule.addCustomSlot(slot: Slot) {
    this.addSlot(
      weekNumber = slot.weekNumber,
      slotTimes = Pair(slot.customStartTime!!, slot.customEndTime!!),
      daysOfWeek = slot.daysOfWeek,
      experimentalMode = experimentalMode,
      timeSlot = slot.timeSlot,
    )
  }

  private fun ActivitySchedule.addRegimeSlot(slot: Slot, timeSlot: Map<TimeSlot, SlotTimes>, daysOfWeekToApply: Set<DayOfWeek>) {
    this.addSlot(
      weekNumber = slot.weekNumber,
      slotTimes = timeSlot[slot.timeSlot]!!,
      daysOfWeek = daysOfWeekToApply,
      experimentalMode = experimentalMode,
      timeSlot = slot.timeSlot,
    )
  }

  fun addScheduleInstances(
    schedule: ActivitySchedule,
    daysToSchedule: List<LocalDate>? = null,
  ) = schedule.addInstances(daysToSchedule)

  /*
   * Note: we add instances even if the activity hasn't started for unlock list purposes.
   */
  private fun ActivitySchedule.addInstances(daysToSchedule: List<LocalDate>? = null) {
    val possibleDaysToSchedule = daysToSchedule ?: LocalDate.now().plusDays(1).let { tomorrow ->
      tomorrow.datesUntil(tomorrow.plusDays(daysInAdvance)).toList()
    }

    possibleDaysToSchedule.filter(::isActiveOn).forEach { activeDay ->
      val scheduleWeekNumber = this.getWeekNumber(activeDay)

      this.slots().filter { slot -> slot.weekNumber == scheduleWeekNumber }.forEach { slot ->
        if (activeDay.dayOfWeek in slot.getDaysOfWeek() &&
          this.hasNoInstancesOnDate(activeDay, slot.slotTimes()) &&
          (runsOnBankHoliday || !bankHolidayService.isEnglishBankHoliday(activeDay))
        ) {
          this.addInstance(sessionDate = activeDay, slot = slot)
        }
      }
    }
  }

  private fun getLocationForSchedule(activity: Activity, locationId: Long?) =
    prisonApiClient.getLocation(locationId!!).block()!!.also { failIfPrisonsDiffer(activity, it) }

  private fun failIfPrisonsDiffer(activity: Activity, location: Location) {
    if (activity.prisonCode != location.agencyId) {
      throw IllegalArgumentException("The activities prison '${activity.prisonCode}' does not match that of the locations '${location.agencyId}'")
    }
  }

  @Transactional
  fun updateActivity(
    prisonCode: String,
    activityId: Long,
    request: ActivityUpdateRequest,
    updatedBy: String,
    adminMode: Boolean? = false,
  ): ModelActivity {
    if (adminMode == false) checkCaseloadAccess(prisonCode)

    transactionHandler.newSpringTransaction {
      val activity = activityRepository.findByActivityIdAndPrisonCodeWithFilters(activityId, prisonCode, LocalDate.now())
        ?: throw EntityNotFoundException("Activity $activityId not found.")
      val updatedAllocationIds = mutableSetOf<Long>()

      require(activity.state(ActivityState.ARCHIVED).not()) {
        "Activity cannot be updated because it is now archived."
      }

      applyCategoryUpdate(request, activity)
      applyTierUpdate(request, activity)
      applyOrganiserUpdate(request, activity)
      applySummaryUpdate(request, activity)
      applyStartDateUpdate(request, activity)
      applyEndDateUpdate(request, activity)
      applyRunsOnBankHolidayUpdate(request, activity)
      applyCapacityUpdate(request, activity)
      applyRiskLevelUpdate(request, activity)
      applyLocationUpdate(request, activity)
      applyAttendanceRequiredUpdate(request, activity)
      applyMinimumEducationLevelUpdate(request, activity)
      applyPayUpdate(request, activity).let { updatedAllocationIds.addAll(it) }
      applyScheduleWeeksUpdate(request, activity)
      applySlotsUpdate(request, activity).let { updatedAllocationIds.addAll(it) }

      if (activity.paid && !activity.attendanceRequired) {
        throw IllegalArgumentException("Activity '$activityId' cannot be paid as attendance is not required.")
      }

      val now = LocalDateTime.now()

      activity.updatedTime = now
      activity.updatedBy = updatedBy

      activity.schedules().forEach {
        it.updateInstances()
        it.markAsUpdated(now, updatedBy)
      }

      activityRepository.saveAndFlush(activity)

      updatedAllocationIds to transform(activity)
    }.let { (updatedAllocationIds, activity) ->
      publishUpdateTelemetryEvent(activity)

      activity.schedules.forEach {
        outboundEventsService.send(OutboundEvent.ACTIVITY_SCHEDULE_UPDATED, it.id)
      }

      updatedAllocationIds.forEach {
        outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, it)
      }

      return activity
    }
  }

  private fun ActivitySchedule.markAsUpdated(
    updated: LocalDateTime,
    updatedBy: String,
  ) {
    this.updatedTime = updated
    this.updatedBy = updatedBy
  }

  private fun applyCategoryUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.categoryId?.apply {
      activity.activityCategory = activityCategoryRepository.findOrThrowIllegalArgument(this)
    }
  }

  private fun applyTierUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.tierCode?.apply {
      if (activity.activityCategory.isNotInWork() && EventTierType.valueOf(request.tierCode) != EventTierType.FOUNDATION) {
        throw IllegalArgumentException("Activity category NOT IN WORK for activity '${activity.activityId}' must be a Foundation Tier.")
      }

      activity.activityTier = eventTierRepository.findByCodeOrThrowIllegalArgument(this)
      if (!activity.activityTier.isTierTwo()) {
        activity.organiser = null
      }
    }
  }

  private fun applyOrganiserUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.organiserCode?.apply {
      activity.organiser = eventOrganiserRepository.findByCodeOrThrowIllegalArgument(this)
    }
  }

  private fun applySummaryUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    if (activity.summary != request.summary) {
      request.summary?.apply {
        failDuplicateActivity(activity.prisonCode, this)
        activity.summary = this
        activity.description = this
        activity.schedules().forEach { it.description = this }
      }
    }
  }

  private fun applyStartDateUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.startDate?.let { newStartDate ->
      val now = LocalDate.now()

      require(activity.startDate.isAfter(now)) { "Activity start date cannot be changed. Activity already started." }
      require(newStartDate.isAfter(now)) { "Activity start date cannot be changed. Start date must be in the future." }
      require(activity.endDate == null || newStartDate <= activity.endDate) {
        "Activity start date cannot be changed. Start date cannot be after the end date."
      }
      require(
        activity.schedules()
          .flatMap { it.allocations(excludeEnded = true) }
          .none { allocation -> newStartDate.isAfter(allocation.startDate) },
      ) {
        "Activity start date cannot be changed. One or more allocations start before the new start date."
      }

      activity.startDate = newStartDate
      activity.schedules().forEach { it.startDate = newStartDate }
    }
  }

  private fun applyEndDateUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    require(request.endDate == null || !request.removeEndDate) {
      "removeEndDate flag cannot be true when an endDate is also supplied."
    }

    if (request.removeEndDate) {
      activity.endDate = null
      activity.schedules().forEach { it.endDate = null }
    } else {
      request.endDate?.let { newEndDate ->
        activity.endDate = newEndDate
        activity.schedules().forEach { it.endDate = newEndDate }
      }
    }
  }

  private fun applyRunsOnBankHolidayUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.runsOnBankHoliday?.let { runsOnBankHoliday ->
      activity.schedules().forEach { it.runsOnBankHoliday = runsOnBankHoliday }
    }
  }

  private fun ActivitySchedule.updateInstances() {
    this.removeRedundantInstances()
    this.addInstances()
  }

  private fun ActivitySchedule.removeRedundantInstances() {
    // Remove any instances that are in the future (not included today) and are no longer required
    // Considers start date, end date, slots and bank holidays
    val instancesToRemove = this.instances().filter {
      it.sessionDate > LocalDate.now()
    }.filter {
      !this.isActiveOn(it.sessionDate) ||
        this.slots().none { slot ->
          slot.weekNumber == this.getWeekNumber(it.sessionDate) &&
            slot.getDaysOfWeek().contains(it.dayOfWeek()) &&
            it.slotTimes() == slot.slotTimes()
        } ||
        !this.runsOnBankHoliday && bankHolidayService.isEnglishBankHoliday(it.sessionDate)
    }

    if (instancesToRemove.isNotEmpty()) this.removeInstances(instancesToRemove)
  }

  private fun applyCapacityUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.capacity?.apply {
      activity.schedules().forEach { it.capacity = this }
    }
  }

  private fun applyRiskLevelUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.riskLevel?.apply {
      activity.riskLevel = this
    }
  }

  private fun applyLocationUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    if (request.locationId == null && request.onWing == null && request.offWing == null && request.inCell == null) {
      return
    }

    require((request.locationId != null) xor (request.onWing == true) xor (request.inCell == true) xor (request.offWing == true)) { "Activity location can only be maximum one of offWing, onWing, inCell, or a specified location" }

    request.locationId?.apply {
      val scheduleLocation = getLocationForSchedule(activity, this)
      activity.schedules().forEach {
        it.internalLocationId = scheduleLocation.locationId.toInt()
        it.internalLocationCode = scheduleLocation.internalLocationCode
        it.internalLocationDescription = scheduleLocation.description
      }
    }

    request.inCell?.apply {
      activity.inCell = this
    }

    request.onWing?.apply {
      activity.onWing = this
    }

    request.offWing?.apply {
      activity.offWing = this
    }
  }

  private fun applyAttendanceRequiredUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.attendanceRequired?.apply {
      activity.activityTier.let { tier ->
        val updateNotAllowed = !tier.isFoundation() &&
          activity.attendanceRequired && request.attendanceRequired == false

        require(!updateNotAllowed) {
          "Attendance cannot be from YES to NO for a '${activity.activityTier.description}' activity."
        }
      }
      activity.attendanceRequired = this
    }
  }

  private fun applyMinimumEducationLevelUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.minimumEducationLevel?.let { minimumEducationLevel ->
      checkEducationLevels(minimumEducationLevel)

      activity.activityMinimumEducationLevel().filter {
        minimumEducationLevel.none { newEducation ->
          it.studyAreaCode == newEducation.studyAreaCode && it.educationLevelCode == newEducation.educationLevelCode
        }
      }.forEach { activity.removeMinimumEducationLevel(it) }

      minimumEducationLevel.filter {
        activity.activityMinimumEducationLevel().none { activityEducation ->
          it.studyAreaCode == activityEducation.studyAreaCode && it.educationLevelCode == activityEducation.educationLevelCode
        }
      }.forEach {
        activity.addMinimumEducationLevel(
          educationLevelCode = it.educationLevelCode!!,
          educationLevelDescription = it.educationLevelDescription!!,
          studyAreaCode = it.studyAreaCode!!,
          studyAreaDescription = it.studyAreaDescription!!,
        )
      }
    }
  }

  private fun applyPayUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ): AllocationIds {
    request.paid?.let { activity.paid = request.paid }

    request.pay?.let { pay ->
      val prisonPayBands = prisonPayBandRepository.findByPrisonCode(activity.prisonCode)
        .associateBy { it.prisonPayBandId }
        .ifEmpty { throw IllegalArgumentException("No pay bands found for prison '${activity.prisonCode}") }

      return replacePayBandAllocationBeforePayRemoval(activity.prisonCode, pay, activity, prisonPayBands).also {
        activity.removePay()
        pay.forEach {
          activity.addPay(
            incentiveNomisCode = it.incentiveNomisCode!!,
            incentiveLevel = it.incentiveLevel!!,
            payBand = prisonPayBands[it.payBandId]
              ?: throw IllegalArgumentException("Pay band not found for prison '${activity.prisonCode}'"),
            rate = it.rate,
            pieceRate = it.pieceRate,
            pieceRateItems = it.pieceRateItems,
            startDate = it.startDate,
          )
        }
      }
    }

    if (request.paid != null && activity.paid && activity.activityPay().isEmpty()) throw IllegalStateException("Activity '${activity.activityId}' must have at least one pay rate.")

    return emptySet()
  }

  private fun replacePayBandAllocationBeforePayRemoval(
    prisonCode: String,
    newPay: List<ActivityPayCreateRequest>,
    activity: Activity,
    prisonPayBands: Map<Long, PrisonPayBand>,
  ): AllocationIds {
    val updatedAllocationIds = mutableSetOf<Long>()
    val oldPay = activity.activityPay()
    val deltaPayBand = newPay.find {
      oldPay.none { p -> p.incentiveNomisCode == it.incentiveNomisCode && p.payBand.prisonPayBandId == it.payBandId }
    }

    deltaPayBand?.let {
      activity.schedules().forEach { schedule ->
        val activeAllocations = schedule.allocations(excludeEnded = true)
        val prisoners = prisonerSearchApiClient.findByPrisonerNumbers(activeAllocations.map { it.prisonerNumber })

        activeAllocations.forEach { allocation ->
          val prisoner = prisoners.single { it.prisonerNumber == allocation.prisonerNumber }
          if (newPay.none { pay -> pay.incentiveNomisCode == prisoner.currentIncentive?.level?.code && pay.payBandId == allocation.payBand!!.prisonPayBandId }) {
            allocation.apply {
              this.payBand = prisonPayBands[it.payBandId]
                ?: throw IllegalArgumentException("Pay band not found for prison '$prisonCode'")
            }
            updatedAllocationIds.add(allocation.allocationId)
          }
        }
      }
    }

    return updatedAllocationIds
  }

  private fun applyScheduleWeeksUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.scheduleWeeks?.let { scheduleWeeks ->
      activity.schedules().forEach { it.scheduleWeeks = scheduleWeeks }
    }
  }

  private fun applySlotsUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ): AllocationIds {
    val updatedAllocationIds = mutableSetOf<Long>()
    request.slots?.let { slots ->
      require(slots.isNotEmpty()) { "Must have at least 1 active slot across the schedule" }
      activity.schedules().forEach { schedule ->
        schedule.usePrisonRegimeTime = slots.all { s -> s.customStartTime == null && s.customEndTime == null }
        schedule.removeSlots()
        schedule.addSlots(slots)
        val activeAllocations = schedule.allocations(excludeEnded = true)
        activeAllocations.forEach { allocation -> allocation.syncExclusionsWithScheduleSlots(schedule.slots())?.let { updatedAllocationIds.add(it) } }
      }
    }
    return updatedAllocationIds
  }
}
