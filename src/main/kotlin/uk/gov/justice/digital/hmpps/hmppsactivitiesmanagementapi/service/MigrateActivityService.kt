package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisScheduleRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AllocationMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot

const val MIGRATION_USER = "MIGRATION"
const val TIER2_IN_CELL_ACTIVITY = "T2ICA"
const val ON_WING_LOCATION = "WOW"
const val DEFAULT_NOMIS_PAY_BAND = "1"
const val DEFAULT_RISK_LEVEL = "low"
const val MAX_ACTIVITY_SUMMARY_SIZE = 50

val commonIncentiveLevels = listOf("BAS", "STD", "ENH")
val prisonsWithASplitRegime = listOf("RSI")
val cohortNames = mapOf(
  "RSI" to "Group",
  "LEI" to "Tranche",
).withDefault { "Group" }

@Service
@Transactional(readOnly = true)
class MigrateActivityService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val activityRepository: ActivityRepository,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val activityTierRepository: ActivityTierRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
  private val prisonPayBandRepository: PrisonPayBandRepository,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun migrateActivity(request: ActivityMigrateRequest): ActivityMigrateResponse {
    if (!rolloutPrisonService.getByPrisonCode(request.prisonCode).activitiesRolledOut) {
      throw ValidationException("The requested prison ${request.prisonCode} is not rolled-out for activities")
    }

    val payBands = prisonPayBandRepository.findByPrisonCode(request.prisonCode)

    // Prison specific rules
    val splitRegime = isSplitRegimeActivity(request)

    log.info("Migrating activity ${request.description} - split regime candidate $splitRegime")

    // Build one or two activities
    val activityList = if (splitRegime) {
      buildSplitActivity(request)
    } else {
      buildSingleActivity(request)
    }

    activityList.forEach { activity ->

      // Add the pay rates provided
      request.payRates.forEach {
        val payBand = payBands.find { pb -> pb.nomisPayBand.toString() == it.nomisPayBand }
        if (payBand != null) {
          activity.addPay(it.incentiveLevel, mapIncentiveLevel(it.incentiveLevel), payBand, it.rate, null, null)
        } else {
          logAndThrowValidationException("Failed to migrate activity ${request.description}. No prison pay band for Nomis pay band ${it.nomisPayBand}")
        }
      }

      // If no pay rates are provided then add a flat rate of 0.00 for all pay band/incentive level combinations
      if (request.payRates.isEmpty()) {
        payBands.forEach { pb ->
          commonIncentiveLevels.forEach { incentive ->
            activity.addPay(incentive, mapIncentiveLevel(incentive), pb, 0, null, null)
          }
        }
      }
    }

    val savedActivities = activityRepository.saveAllAndFlush(activityList)

    log.info("Migrated activity ${request.description}")

    val splitRegimeActivityId = if (savedActivities.size > 1) savedActivities.last().activityId else null

    return ActivityMigrateResponse(request.prisonCode, savedActivities.first().activityId, splitRegimeActivityId)
  }

  // TODO: Tests for this
  fun isSplitRegimeActivity(request: ActivityMigrateRequest): Boolean {
    // TODO: Prison-specific rules to identify if this is a split regime activity

    // Ignore prisons which do not operate a split regime
    if (prisonsWithASplitRegime.find { it == request.prisonCode }.isNullOrEmpty()) {
      return false
    }

    // Has both morning and afternoon slots
    // Has no evening slots
    // Type SAA_EDUCATION or SAA_INDUSTRIES
    // Has some pay rates
    // Is in a list of descriptions provided by the prison?

    return false
  }

  fun buildSingleActivity(request: ActivityMigrateRequest): List<Activity> {
    val activity = buildActivityEntity(request)
    request.scheduleRules.forEach {
      activity.schedules().first().addSlot(1, it.startTime, it.endTime, getRequestDaysOfWeek(it))
    }
    return listOf(activity)
  }

  // TODO: Tests for this
  fun buildSplitActivity(request: ActivityMigrateRequest): List<Activity> {
    val activity1 = buildActivityEntity(request, true, 2, 1)

    // TODO: What if there are ED slots on a split regime activity??

    // Add the morning sessions to week 1
    request.scheduleRules.forEach {
      if (TimeSlot.slot(it.startTime) == TimeSlot.AM) {
        activity1.schedules().first().addSlot(1, it.startTime, it.endTime, getRequestDaysOfWeek(it))
      }
    }

    // Add the afternoon sessions to week 2
    request.scheduleRules.forEach {
      if (TimeSlot.slot(it.startTime) == TimeSlot.PM) {
        activity1.schedules().first().addSlot(2, it.startTime, it.endTime, getRequestDaysOfWeek(it))
      }
    }

    val activity2 = buildActivityEntity(request, true, 2, 2)

    // Add the afternoon sessions to week 1
    request.scheduleRules.forEach {
      if (TimeSlot.slot(it.startTime) == TimeSlot.PM) {
        activity2.schedules().first().addSlot(1, it.startTime, it.endTime, getRequestDaysOfWeek(it))
      }
    }

    // Add the morning sessions to week 2
    request.scheduleRules.forEach {
      if (TimeSlot.slot(it.startTime) == TimeSlot.AM) {
        activity2.schedules().first().addSlot(2, it.startTime, it.endTime, getRequestDaysOfWeek(it))
      }
    }

    return listOf(activity1, activity2)
  }

  private fun buildActivityEntity(
    request: ActivityMigrateRequest,
    splitRegime: Boolean = false,
    scheduledWeeks: Int = 1,
    cohort: Int? = null,
  ): Activity {
    val tomorrow = LocalDate.now().plusDays(1)

    return Activity(
      prisonCode = request.prisonCode,
      activityCategory = mapProgramToCategory(request.programServiceCode),
      activityTier = mapProgramToTier(request.programServiceCode),
      attendanceRequired = true,
      summary = makeNameWithCohortLabel(splitRegime, request.prisonCode, request.description, cohort),
      description = "Migrated from NOMIS with program service code ${request.programServiceCode}",
      inCell = (request.internalLocationId == null && !request.outsideWork) || request.programServiceCode == TIER2_IN_CELL_ACTIVITY,
      onWing = request.internalLocationCode?.contains(ON_WING_LOCATION) ?: false,
      outsideWork = request.outsideWork,
      startDate = tomorrow,
      riskLevel = DEFAULT_RISK_LEVEL,
      minimumIncentiveNomisCode = request.minimumIncentiveLevel,
      minimumIncentiveLevel = mapIncentiveLevel(request.minimumIncentiveLevel),
      createdTime = LocalDateTime.now(),
      createdBy = MIGRATION_USER,
      updatedTime = LocalDateTime.now(),
      updatedBy = MIGRATION_USER,
    ).apply {
      endDate = request.endDate
    }.apply {
      addSchedule(
        description = this.summary,
        internalLocation = request.internalLocationId?.let {
          Location(
            locationId = it,
            internalLocationCode = request.internalLocationCode,
            description = request.internalLocationDescription!!,
            locationType = "N/A",
            agencyId = request.prisonCode,
          )
        },
        capacity = request.capacity,
        startDate = this.startDate,
        endDate = this.endDate,
        runsOnBankHoliday = request.runsOnBankHoliday,
        scheduleWeeks = scheduledWeeks,
      )
    }
  }

  // TODO: Tests for this
  fun makeNameWithCohortLabel(splitRegime: Boolean, prisonCode: String, description: String, cohort: Int? = null): String {
    if (!splitRegime) {
      return description
    }

    val cohortLabel = cohortNames.getValue(prisonCode)
    val labelSize = cohortLabel.length + 3
    return if ((description.trim().length + labelSize) <= MAX_ACTIVITY_SUMMARY_SIZE)
      "${description.trim()} $cohortLabel $cohort"
    else
      "${description.trim().slice(IntRange(0, (MAX_ACTIVITY_SUMMARY_SIZE - labelSize)))} $cohortLabel $cohort"
  }

  fun getRequestDaysOfWeek(nomisSchedule: NomisScheduleRule): Set<DayOfWeek> {
    return setOfNotNull(
      DayOfWeek.MONDAY.takeIf { nomisSchedule.monday },
      DayOfWeek.TUESDAY.takeIf { nomisSchedule.tuesday },
      DayOfWeek.WEDNESDAY.takeIf { nomisSchedule.wednesday },
      DayOfWeek.THURSDAY.takeIf { nomisSchedule.thursday },
      DayOfWeek.FRIDAY.takeIf { nomisSchedule.friday },
      DayOfWeek.SATURDAY.takeIf { nomisSchedule.saturday },
      DayOfWeek.SUNDAY.takeIf { nomisSchedule.sunday },
    )
  }

  // TODO: Temporary - will work for Risley but we should look these up from incentives API for other prisons
  fun mapIncentiveLevel(code: String): String {
    val incentiveLevel = when (code) {
      "BAS" -> "Basic"
      "STD" -> "Standard"
      "ENH" -> "Enhanced"
      "EN2" -> "Enhanced2"
      else -> "Unknown"
    }
    return incentiveLevel
  }

  fun mapProgramToCategory(programServiceCode: String): ActivityCategory {
    val activityCategories = activityCategoryRepository.findAll()
    val category = when {
      // Prison industries
      programServiceCode.startsWith("IND_") -> activityCategories.isIndustries()

      // Prison jobs
      programServiceCode.startsWith("SER_") -> activityCategories.isPrisonJobs()
      programServiceCode.startsWith("KITCHEN") -> activityCategories.isPrisonJobs()
      programServiceCode.startsWith("CLNR") -> activityCategories.isPrisonJobs()
      programServiceCode.startsWith("FG") -> activityCategories.isPrisonJobs()
      programServiceCode.startsWith("LIBRARY") -> activityCategories.isPrisonJobs()
      programServiceCode.startsWith("WORKS") -> activityCategories.isPrisonJobs()
      programServiceCode.startsWith("RECYCLE") -> activityCategories.isPrisonJobs()

      // Education
      programServiceCode.startsWith("EDU") -> activityCategories.isEducation()
      programServiceCode.startsWith("CORECLASS") -> activityCategories.isEducation()
      programServiceCode.startsWith("SKILLS") -> activityCategories.isEducation()
      programServiceCode.startsWith("KEY_SKILLS") -> activityCategories.isEducation()

      // Not in work
      programServiceCode.startsWith("UNEMP") -> activityCategories.isNotInWork()
      programServiceCode.startsWith("OTH_UNE") -> activityCategories.isNotInWork()

      // Interventions/courses
      programServiceCode.startsWith("INT_") -> activityCategories.isInterventions()
      programServiceCode.startsWith("GROUP") -> activityCategories.isInterventions()
      programServiceCode.startsWith("ABUSE") -> activityCategories.isInterventions()

      // Sports and fitness
      programServiceCode.startsWith("PE") -> activityCategories.isGymSportsFitness()
      programServiceCode.startsWith("SPORT") -> activityCategories.isGymSportsFitness()
      programServiceCode.startsWith("HEALTH") -> activityCategories.isGymSportsFitness()
      programServiceCode.startsWith("T2PER") -> activityCategories.isGymSportsFitness()
      programServiceCode.startsWith("T2HCW") -> activityCategories.isGymSportsFitness()
      programServiceCode.startsWith("OTH_PER") -> activityCategories.isGymSportsFitness()

      // Faith and spirituality
      programServiceCode.startsWith("CHAP") -> activityCategories.isFaithSpirituality()
      programServiceCode.startsWith("T2CFA") -> activityCategories.isFaithSpirituality()
      programServiceCode.startsWith("OTH_CFR") -> activityCategories.isFaithSpirituality()

      // Induction/guidance
      programServiceCode.startsWith("INDUCTION") -> activityCategories.isInduction()
      programServiceCode.startsWith("IAG") -> activityCategories.isInduction()
      programServiceCode.startsWith("SAFE") -> activityCategories.isInduction()

      // Everything else is Other
      else -> activityCategories.isOther()
    }

    return category ?: throw ValidationException("Could not map $programServiceCode to a category")
  }

  // Helper functions
  fun List<ActivityCategory>.isIndustries() = this.find { it.code == "SAA_INDUSTRIES" }
  fun List<ActivityCategory>.isPrisonJobs() = this.find { it.code == "SAA_PRISON_JOBS" }
  fun List<ActivityCategory>.isEducation() = this.find { it.code == "SAA_EDUCATION" }
  fun List<ActivityCategory>.isNotInWork() = this.find { it.code == "SAA_NOT_IN_WORK" }
  fun List<ActivityCategory>.isInterventions() = this.find { it.code == "SAA_INTERVENTIONS" }
  fun List<ActivityCategory>.isInduction() = this.find { it.code == "SAA_INDUCTION" }
  fun List<ActivityCategory>.isGymSportsFitness() = this.find { it.code == "SAA_GYM_SPORTS_FITNESS" }
  fun List<ActivityCategory>.isFaithSpirituality() = this.find { it.code == "SAA_FAITH_SPIRITUALITY" }
  fun List<ActivityCategory>.isOther() = this.find { it.code == "SAA_OTHER" }

  fun mapProgramToTier(programServiceCode: String): ActivityTier? {
    // We only have one tier
    val tiers = activityTierRepository.findAll()
    return if (tiers.size > 0) tiers.first() else null
  }

  @Transactional
  fun migrateAllocation(request: AllocationMigrateRequest): AllocationMigrateResponse {
    if (!rolloutPrisonService.getByPrisonCode(request.prisonCode).activitiesRolledOut) {
      logAndThrowValidationException("Prison ${request.prisonCode} is not rolled out for activities")
    }

    // Is split regime? Find both activities

    val activity = activityRepository.findByActivityIdAndPrisonCode(request.activityId, request.prisonCode)
      ?: logAndThrowValidationException("Activity ${request.activityId} was not found at prison ${request.prisonCode}")

    log.info("Migrating allocation ${request.prisonerNumber} to activity ${request.activityId} ${activity.summary}")

    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)

    if (!activity.isActive(tomorrow)) {
      logAndThrowValidationException("Allocation failed ${request.prisonerNumber}. ${request.activityId} ${activity.summary} activity is not active tomorrow")
    }

    val activityScheduleId = activity.schedules().first().activityScheduleId
    val schedule = activityScheduleRepository.findBy(activityScheduleId, activity.prisonCode)
      ?: logAndThrowValidationException("Allocation failed ${request.prisonerNumber} to ${request.activityId} ${activity.summary}. Activity schedule ID $activityScheduleId not found.")

    if (!schedule.isActiveOn(tomorrow)) {
      logAndThrowValidationException("Allocation failed ${request.prisonerNumber}. ${request.activityId} ${activity.summary} - schedule is not active tomorrow")
    }

    if (schedule.allocations(excludeEnded = true).any { allocation -> allocation.prisonerNumber == request.prisonerNumber }) {
      logAndThrowValidationException("Allocation failed ${request.prisonerNumber}. Already allocated to ${request.activityId} ${activity.summary}")
    }

    val prisonerResults = prisonerSearchApiClient.findByPrisonerNumbers(listOf(request.prisonerNumber)).block()
    if (prisonerResults.isNullOrEmpty()) {
      logAndThrowValidationException("Allocation failed ${request.prisonerNumber}. Prisoner not found in prisoner search.")
    }

    val prisoner = prisonerResults.first()
    if (prisoner.prisonId != request.prisonCode || prisoner.status.contains("INACTIVE")) {
      logAndThrowValidationException("Allocation failed ${request.prisonerNumber}. Prisoner not in ${request.prisonCode} or INACTIVE")
    }

    val payBands = prisonPayBandRepository.findByPrisonCode(request.prisonCode)

    val prisonPayBand = if (request.nomisPayBand.isNullOrEmpty()) {
      payBands.find { "${it.nomisPayBand}".trim() == DEFAULT_NOMIS_PAY_BAND }
        ?: logAndThrowValidationException("Allocation failed ${request.prisonerNumber}. Pay band default $DEFAULT_NOMIS_PAY_BAND is not valid for ${request.prisonCode}")
    } else {
      payBands.find { "${it.nomisPayBand}" == request.nomisPayBand }
        ?: logAndThrowValidationException("Allocation failed ${request.prisonerNumber}. Nomis pay band ${request.nomisPayBand} is not configured for ${request.prisonCode}")
    }

    schedule.allocatePrisoner(
      prisonerNumber = request.prisonerNumber.toPrisonerNumber(),
      payBand = prisonPayBand,
      bookingId = prisoner.bookingId?.let { prisoner.bookingId.toLong() } ?: 0L,
      startDate = if (request.startDate.isAfter(tomorrow)) request.startDate else tomorrow,
      endDate = request.endDate,
      allocatedBy = MIGRATION_USER,
    )

    if (request.suspendedFlag) {
      log.info("SUSPENDED prisoner ${request.prisonerNumber} being allocated to ${activity.activityId} ${activity.summary} as PENDING")
    }

    val savedSchedule = activityScheduleRepository.saveAndFlush(schedule)

    val allocation = savedSchedule.allocations(excludeEnded = true)
      .find { it.prisonerNumber == request.prisonerNumber }
      ?: logAndThrowValidationException("Allocation failed ${request.prisonerNumber}. Could not re-read the saved allocation")

    log.info("Allocated ${request.prisonerNumber} to ${activity.activityId} ${activity.summary} with allocation ID ${allocation.allocationId}")

    return AllocationMigrateResponse(request.activityId, allocation.allocationId)
  }

  private fun logAndThrowValidationException(msg: String): Nothing {
    log.error(msg)
    throw ValidationException(msg)
  }

  @PreAuthorize("hasAnyRole('NOMIS_ACTIVITIES')")
  @Transactional
  fun deleteActivityCascade(prisonCode: String, activityId: Long) {
    log.info("Delete cascade activity ID $activityId")
    val activity = activityRepository.findByActivityIdAndPrisonCode(activityId, prisonCode)
      ?: logAndThrowValidationException("Failed to delete. Activity $activityId was not found at prison $prisonCode")
    activityRepository.delete(activity)
  }
}
