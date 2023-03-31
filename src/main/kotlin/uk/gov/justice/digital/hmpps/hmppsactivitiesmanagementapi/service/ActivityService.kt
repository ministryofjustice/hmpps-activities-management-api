package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.toModelLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
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
  ) = activityRepository.getAllByPrisonCode(prisonCode).toModelLite()

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
    checkEducationLevels(request)

    val activity = Activity(
      prisonCode = request.prisonCode,
      activityCategory = category,
      activityTier = tier,
      attendanceRequired = request.attendanceRequired,
      summary = request.summary,
      description = request.description,
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
        )
      }
    }

    activity.let {
      val scheduleLocation = getLocationForSchedule(it, request)

      val prisonRegime = prisonRegimeService.getPrisonRegimeByPrisonCode(activity.prisonCode)
      val timeSlots =
        mapOf(
          TimeSlot.AM to Pair(prisonRegime.amStart, prisonRegime.amFinish),
          TimeSlot.PM to Pair(prisonRegime.pmStart, prisonRegime.pmFinish),
          TimeSlot.ED to Pair(prisonRegime.edStart, prisonRegime.edFinish),
        )

      activity.addSchedule(
        description = request.description!!,
        internalLocationId = scheduleLocation.locationId.toInt(),
        internalLocationCode = scheduleLocation.internalLocationCode ?: "",
        internalLocationDescription = scheduleLocation.description,
        capacity = request.capacity!!,
        startDate = request.startDate!!,
        endDate = request.endDate,
        runsOnBankHoliday = request.runsOnBankHoliday,
      ).let { schedule ->
        schedule.addSlots(request.slots!!, timeSlots)
        schedule.addInstances(activity, schedule.slots())

        return transform(activityRepository.saveAndFlush(activity))
      }
    }
  }

  private fun checkEducationLevels(request: ActivityCreateRequest) {
    request.minimumEducationLevel.forEach {
      val educationLevelCode = it.educationLevelCode!!
      val educationLevel = prisonApiClient.getEducationLevel(educationLevelCode).block()!!
      if (educationLevel.activeFlag != "Y") {
        throw IllegalArgumentException("The education level code '$educationLevelCode' is not active in NOMIS")
      } else {
        failIfDescriptionDiffers(it.educationLevelDescription!!, educationLevel.description)
      }
    }
  }

  private fun failIfDescriptionDiffers(requestDescription: String, apiDescription: String?) {
    if (requestDescription != apiDescription) {
      throw IllegalArgumentException("The education level description '$requestDescription' does not match that of the NOMIS education level '$apiDescription'")
    }
  }

  private fun ActivitySchedule.addSlots(slots: List<Slot>, timeSlots: Map<TimeSlot, Pair<LocalTime, LocalTime>>) {
    slots.forEach { slot ->
      val (start, end) = timeSlots[TimeSlot.valueOf(slot.timeSlot!!)]!!

      val daysOfWeek = getDaysOfWeek(slot)

      this.addSlot(start, end, daysOfWeek)
    }
  }

  private fun ActivitySchedule.addInstances(activity: Activity, slots: List<ActivityScheduleSlot>) {
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
            )
        ) {
          this.addInstance(sessionDate = day, slot = slot)
        }
      }
    }
  }

  private fun getLocationForSchedule(activity: Activity, request: ActivityCreateRequest) =
    prisonApiClient.getLocation(request.locationId!!).block()!!.also { failIfPrisonsDiffer(activity, it) }

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
}
