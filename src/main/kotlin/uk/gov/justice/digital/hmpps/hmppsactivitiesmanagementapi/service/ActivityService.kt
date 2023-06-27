package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModelLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMinimumEducationLevelCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityPayCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EligibilityRuleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.findOrThrowNotFound
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity as ModelActivity

@Service
class ActivityService(
  private val activityRepository: ActivityRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
  private val activityTierRepository: ActivityTierRepository,
  private val eligibilityRuleRepository: EligibilityRuleRepository,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val prisonPayBandRepository: PrisonPayBandRepository,
  private val prisonApiClient: PrisonApiClient,
  private val prisonRegimeService: PrisonRegimeService,
  private val bankHolidayService: BankHolidayService,
  @Value("\${online.create-scheduled-instances.days-in-advance}") private val daysInAdvance: Long = 14L,
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getActivityById(activityId: Long) =
    transform(
      activityRepository.findOrThrowNotFound(activityId),
    )

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
  ) = activityRepository.getAllByPrisonCode(prisonCode)
    .filter { !excludeArchived || !it.state(ActivityState.ARCHIVED) }
    .toModelLite()

  fun getSchedulesForActivity(activityId: Long) =
    activityRepository.findOrThrowNotFound(activityId)
      .let { activityScheduleRepository.getAllByActivity(it).toModelLite() }

  private fun failDuplicateActivity(prisonCode: String, summary: String) {
    val duplicateActivity = activityRepository.existsActivityByPrisonCodeAndSummary(prisonCode, summary)
    if (duplicateActivity) {
      throw IllegalArgumentException("Duplicate activity name detected for this prison ($prisonCode): '$summary'")
    }
  }

  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  fun createActivity(request: ActivityCreateRequest, createdBy: String): ModelActivity {
    val category = activityCategoryRepository.findOrThrowIllegalArgument(request.categoryId!!)
    val tier = request.tierId?.let { activityTierRepository.findOrThrowIllegalArgument(it) }
    val eligibilityRules = request.eligibilityRuleIds.map { eligibilityRuleRepository.findOrThrowIllegalArgument(it) }
    val prisonPayBands = prisonPayBandRepository.findByPrisonCode(request.prisonCode!!)
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
      startDate = request.startDate ?: LocalDate.now(),
      endDate = request.endDate,
      riskLevel = request.riskLevel!!,
      minimumIncentiveNomisCode = request.minimumIncentiveNomisCode!!,
      minimumIncentiveLevel = request.minimumIncentiveLevel!!,
      createdTime = LocalDateTime.now(),
      createdBy = createdBy,
    ).apply {
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
      val scheduleLocation = if (request.inCell) null else getLocationForSchedule(it, request.locationId!!)
      val prisonRegime = prisonRegimeService.getPrisonRegimeByPrisonCode(activity.prisonCode)
      val timeSlots =
        mapOf(
          TimeSlot.AM to Pair(prisonRegime.amStart, prisonRegime.amFinish),
          TimeSlot.PM to Pair(prisonRegime.pmStart, prisonRegime.pmFinish),
          TimeSlot.ED to Pair(prisonRegime.edStart, prisonRegime.edFinish),
        )

      activity.addSchedule(
        description = request.description!!,
        internalLocation = scheduleLocation,
        capacity = request.capacity!!,
        startDate = request.startDate!!,
        endDate = request.endDate,
        runsOnBankHoliday = request.runsOnBankHoliday,
      ).let { schedule ->
        schedule.addSlots(request.slots!!, timeSlots)
        schedule.addInstances(activity, schedule.slots(), null, null)

        return transform(activityRepository.saveAndFlush(activity))
      }
    }
  }

  private fun checkEducationLevels(minimumEducationLevels: List<ActivityMinimumEducationLevelCreateRequest>) {
    minimumEducationLevels.forEach {
      val educationLevelCode = it.educationLevelCode!!
      val educationLevel = prisonApiClient.getEducationLevel(educationLevelCode).block()!!
      require(educationLevel.isActive()) { "The education level code '$educationLevelCode' is not active in NOMIS" }
      failIfDescriptionDiffers(it.educationLevelDescription!!, educationLevel.description)

      val studyAreaCode = it.studyAreaCode!!
      val studyArea = prisonApiClient.getStudyArea(studyAreaCode).block()!!
      require(studyArea.isActive()) { "The study area code '$studyAreaCode' is not active in NOMIS" }
      failIfDescriptionDiffers(it.studyAreaDescription!!, studyArea.description)
    }
  }

  private fun failIfDescriptionDiffers(requestDescription: String, apiDescription: String?) {
    require(requestDescription == apiDescription) {
      "The education level description '$requestDescription' does not match that of the NOMIS education level '$apiDescription'"
    }
  }

  private fun ActivitySchedule.addSlots(slots: List<Slot>, timeSlots: Map<TimeSlot, Pair<LocalTime, LocalTime>>) {
    slots.forEach { slot ->
      val (start, end) = timeSlots[TimeSlot.valueOf(slot.timeSlot!!)]!!

      val daysOfWeek = getDaysOfWeek(slot)

      this.addSlot(start, end, daysOfWeek)
    }
  }

  private fun ActivitySchedule.addInstances(
    activity: Activity,
    slots: List<ActivityScheduleSlot>,
    fromDate: LocalDate? = null,
    toDate: LocalDate? = null,
  ) {
    val today = LocalDate.now()
    val endDay = today.plusDays(daysInAdvance)
    val listOfDatesToSchedule = today.datesUntil(endDay).toList()

    listOfDatesToSchedule.forEach { day ->
      slots.forEach { slot ->
        val daysOfWeek = getDaysOfWeek(
          Slot(
            timeSlot = "",
            monday = slot.mondayFlag,
            tuesday = slot.tuesdayFlag,
            wednesday = slot.wednesdayFlag,
            thursday = slot.thursdayFlag,
            friday = slot.fridayFlag,
            saturday = slot.saturdayFlag,
            sunday = slot.sundayFlag,
          ),
        )

        if (activity.isActive(day) && day.dayOfWeek in daysOfWeek &&
          (
            runsOnBankHoliday || !bankHolidayService.isEnglishBankHoliday(day)
            ) &&
          (fromDate == null || day >= fromDate) &&
          (toDate == null || day <= toDate)
        ) {
          if (this.hasNoInstancesOnDate(day, slot.startTime to slot.endTime)) {
            this.addInstance(sessionDate = day, slot = slot)
          }
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

  private fun getDaysOfWeek(slot: Slot): Set<DayOfWeek> {
    return setOfNotNull(
      DayOfWeek.MONDAY.takeIf { slot.monday },
      DayOfWeek.TUESDAY.takeIf { slot.tuesday },
      DayOfWeek.WEDNESDAY.takeIf { slot.wednesday },
      DayOfWeek.THURSDAY.takeIf { slot.thursday },
      DayOfWeek.FRIDAY.takeIf { slot.friday },
      DayOfWeek.SATURDAY.takeIf { slot.saturday },
      DayOfWeek.SUNDAY.takeIf { slot.sunday },
    )
  }

  @PreAuthorize("hasAnyRole('ACTIVITY_HUB', 'ACTIVITY_HUB_LEAD', 'ACTIVITY_ADMIN')")
  fun updateActivity(
    prisonCode: String,
    activityId: Long,
    request: ActivityUpdateRequest,
    updatedBy: String,
  ): ModelActivity {
    val activity = activityRepository.findOrThrowNotFound(activityId)
    val now = LocalDateTime.now()

    applyCategoryUpdate(request, activity)
    applyTierUpdate(request, activity)
    applySummaryUpdate(prisonCode, request, activity)
    applyStartDateUpdate(request, activity)
    applyEndDateUpdate(request, activity)
    applyMinimumIncentiveNomisCodeUpdate(request, activity)
    applyMinimumIncentiveLevelUpdate(request, activity)
    applyRunsOnBankHolidayUpdate(request, activity)
    applyCapacityUpdate(request, activity)
    applyRiskLevelUpdate(request, activity)
    applyLocationUpdate(request, activity)
    applyInCellUpdate(request, activity)
    applyAttendanceRequiredUpdate(request, activity)
    if (request.minimumEducationLevel != null) {
      applyMinimumEducationLevelUpdate(request.minimumEducationLevel, activity)
    }
    if (request.pay != null) {
      applyPayUpdate(prisonCode, request.pay, activity)
    }
    if (request.slots != null) {
      applySlotsUpdate(request.slots, activity)
    }

    activity.updatedTime = now
    activity.updatedBy = updatedBy

    activity.schedules().forEach { it.markAsUpdated(now, updatedBy) }

    activityRepository.saveAndFlush(activity)

    return transform(activity)
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
    request.tierId?.apply {
      activity.activityTier = activityTierRepository.findOrThrowIllegalArgument(this)
    }
  }

  private fun applySummaryUpdate(
    prisonCode: String,
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.summary?.apply {
      failDuplicateActivity(prisonCode, this)
      activity.summary = this
      activity.description = this
      activity.schedules().forEach { it.description = this }
    }
  }

  private fun applyStartDateUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.startDate?.let { newStartDate ->
      activity.startDate = newStartDate
      activity.schedules().forEach {
        if (it.startDate < newStartDate) {
          // start date has been moved later so remove all instances between the original start date and the day before the new start date
          it.removeInstances(it.startDate, newStartDate.minusDays(1))
        } else if (newStartDate < it.startDate) {
          // start date has been moved earlier so create new instances between the new start date and the day before the original start date
          it.addInstances(activity, it.slots(), newStartDate, it.startDate.minusDays(1))
        }
        it.startDate = newStartDate
      }
    }
  }

  private fun applyEndDateUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.endDate?.let { newEndDate ->
      activity.endDate = newEndDate
      activity.schedules().forEach {
        if (it.endDate == null || it.endDate!! > newEndDate) {
          // end date has been set or moved earlier so remove all instances from the day after the new end date
          (it.endDate)?.let { it1 -> it.removeInstances(newEndDate.plusDays(1), it1) }
        } else if (it.endDate !== null && it.endDate!! < newEndDate) {
          // end date has been moved later so add new instances from the day after the original end date up to the new end date
          it.addInstances(activity, it.slots(), it.endDate!!.plusDays(1), newEndDate)
        }
        it.endDate = newEndDate
      }
    }
  }

  private fun applyMinimumIncentiveNomisCodeUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.minimumIncentiveNomisCode?.apply {
      activity.minimumIncentiveNomisCode = this
    }
  }

  private fun applyMinimumIncentiveLevelUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.minimumIncentiveLevel?.apply {
      activity.minimumIncentiveLevel = this
    }
  }

  private fun applyRunsOnBankHolidayUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.runsOnBankHoliday?.apply {
      activity.schedules().forEach { it.runsOnBankHoliday = this }
    }
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
    request.locationId?.apply {
      val scheduleLocation = getLocationForSchedule(activity, this)
      activity.schedules().forEach {
        it.internalLocationId = scheduleLocation.locationId.toInt()
        it.internalLocationCode = scheduleLocation.internalLocationCode
        it.internalLocationDescription = scheduleLocation.description
      }
    }
  }

  private fun applyInCellUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.inCell?.apply {
      activity.inCell = this
    }
  }

  private fun applyAttendanceRequiredUpdate(
    request: ActivityUpdateRequest,
    activity: Activity,
  ) {
    request.attendanceRequired?.apply {
      activity.attendanceRequired = this
    }
  }

  private fun applyMinimumEducationLevelUpdate(
    minimumEducationLevel: List<ActivityMinimumEducationLevelCreateRequest>,
    activity: Activity,
  ) {
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

  private fun applyPayUpdate(
    prisonCode: String,
    pay: List<ActivityPayCreateRequest>,
    activity: Activity,
  ) {
    activity.removePay()
    val prisonPayBands = prisonPayBandRepository.findByPrisonCode(prisonCode)
      .associateBy { it.prisonPayBandId }
      .ifEmpty { throw IllegalArgumentException("No pay bands found for prison '$prisonCode") }
    pay.forEach {
      activity.addPay(
        incentiveNomisCode = it.incentiveNomisCode!!,
        incentiveLevel = it.incentiveLevel!!,
        payBand = prisonPayBands[it.payBandId]
          ?: throw IllegalArgumentException("Pay band not found for prison '$prisonCode'"),
        rate = it.rate,
        pieceRate = it.pieceRate,
        pieceRateItems = it.pieceRateItems,
      )
    }
  }

  private fun applySlotsUpdate(
    slots: List<Slot>,
    activity: Activity,
  ) {
    val prisonRegime = prisonRegimeService.getPrisonRegimeByPrisonCode(activity.prisonCode)
    val timeSlots =
      mapOf(
        TimeSlot.AM to Pair(prisonRegime.amStart, prisonRegime.amFinish),
        TimeSlot.PM to Pair(prisonRegime.pmStart, prisonRegime.pmFinish),
        TimeSlot.ED to Pair(prisonRegime.edStart, prisonRegime.edFinish),
      )

    activity.schedules().forEach {
      it.updateSlotsAndRemoveRedundantInstances(slots.toMap(timeSlots))
      it.addInstances(activity, it.slots())
    }
  }

  private fun List<Slot>.toMap(regimeTimeSlots: Map<TimeSlot, Pair<LocalTime, LocalTime>>) =
    associate { regimeTimeSlots[it.timeSlot()]!! to it.getDaysOfWeek() }
}
