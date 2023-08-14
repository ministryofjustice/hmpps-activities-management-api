package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
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

// Useful constants
const val MIGRATION_USER = "MIGRATION"
const val TIER2_IN_CELL_ACTIVITY = "T2ICA"
const val ON_WING_LOCATION = "WOW"

@Service
@Transactional(readOnly = true)
class MigrateActivityService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val activityRepository: ActivityRepository,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val activityTierRepository: ActivityTierRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
  private val prisonPayBandRepository: PrisonPayBandRepository,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @PreAuthorize("hasAnyRole('NOMIS_ACTIVITIES')")
  @Transactional
  fun migrateActivity(request: ActivityMigrateRequest): ActivityMigrateResponse {
    log.info("Request to migrate NOMIS activity ${request.description}")

    if (!rolloutPrisonService.getByPrisonCode(request.prisonCode).activitiesRolledOut) {
      throw ValidationException("The requested prison is not rolled-out for activities")
    }

    // TODO: Is this a split regime activity? Different path for this ....

    // Create the activity with its schedule from the request
    val activity = buildActivityEntity(request)

    // Add the time slots into the schedule
    request.scheduleRules.forEach {
      activity.schedules().first().addSlot(1, it.startTime, it.endTime, getRequestDaysOfWeek(it))
    }

    // Add the pay rates
    val payBands = prisonPayBandRepository.findByPrisonCode(request.prisonCode)
    request.payRates.forEach {
      val payBand = payBands.find { pb -> pb.nomisPayBand.toString() == it.nomisPayBand }
      if (payBand != null) {
        activity.addPay(it.incentiveLevel, mapIncentiveLevel(it.incentiveLevel), payBand, it.rate, null, null)
      } else {
        val errMsg = "Failed to migrate activity ${request.description}. No matching prison pay band found for Nomis pay band ${it.nomisPayBand}"
        log.error(errMsg)
        throw ValidationException(errMsg)
      }
    }

    // Saving the activity will also save the schedule, and this will trigger the sync event to NOMIS.
    val savedActivity = activityRepository.saveAndFlush(activity)

    return ActivityMigrateResponse(request.prisonCode, savedActivity.activityId, null)
  }

  private fun buildActivityEntity(request: ActivityMigrateRequest): Activity {
    return Activity(
      prisonCode = request.prisonCode,
      activityCategory = mapProgramToCategory(request.programServiceCode),
      activityTier = mapProgramToTier(request.programServiceCode),
      attendanceRequired = true, // Default
      summary = request.description,
      description = "Migrated from NOMIS ${request.description} program service ${request.programServiceCode}",
      inCell = request.internalLocationId == null || request.programServiceCode == TIER2_IN_CELL_ACTIVITY,
      onWing = request.internalLocationCode?.contains(ON_WING_LOCATION) ?: false,
      startDate = LocalDate.now().plusDays(1), // Start date of tomorrow
      riskLevel = "Low", // Default
      minimumIncentiveNomisCode = request.minimumIncentiveLevel,
      minimumIncentiveLevel = mapIncentiveLevel(request.minimumIncentiveLevel),
      createdTime = LocalDateTime.now(),
      createdBy = MIGRATION_USER,
      updatedTime = LocalDateTime.now(),
      updatedBy = MIGRATION_USER,
    ).apply {
      addSchedule(
        description = request.description,
        internalLocation = request.internalLocationId?.let {
          // TODO: Why is this using a prison API type for Location?
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
        endDate = request.endDate,
        runsOnBankHoliday = request.runsOnBankHoliday,
        scheduleWeeks = 1, // Defaults to 1
      )
    }
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

  // TODO: Not happy with this - should we get from the incentives API for this prison?
  // Or in the request from the migration service?
  // It will work for Risley, and most other prisons, except where they have defined their own set.
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
    // We only have one Tier for now - use Tier 1 for all
    // TODO - some of these program code show Tier 2 activities - we could define a Tier 2?
    val tiers = activityTierRepository.findAll()
    return if (tiers.size > 0) tiers.first() else null
  }

  @PreAuthorize("hasAnyRole('NOMIS_ACTIVITIES')")
  @Transactional
  fun migrateAllocation(request: AllocationMigrateRequest): AllocationMigrateResponse {
    log.info("Request to migrate an allocation")

    if (!rolloutPrisonService.getByPrisonCode(request.prisonCode).activitiesRolledOut) {
      throw ValidationException("The requested prison is not rolled-out for activities")
    }

    // Is there a split regime ID provided? If yes, need to use rules to make the choice of activity
    // If no choice, allocate to the activity ID provided.

    return AllocationMigrateResponse(1L, 1L)
  }
}
