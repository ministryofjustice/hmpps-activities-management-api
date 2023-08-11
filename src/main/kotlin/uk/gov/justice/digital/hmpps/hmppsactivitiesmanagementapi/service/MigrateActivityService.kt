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

const val MIGRATION_USER = "MIGRATION"

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
    val log: Logger = LoggerFactory.getLogger(this::class.java)
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
        val errMsg = "No matching pay band found for ${request.description} ${it.nomisPayBand}"
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
      description = request.description,
      inCell = request.internalLocationId == null || request.programServiceCode == "T2ICA",
      onWing = request.internalLocationCode?.contains("WOW") ?: false,
      startDate = request.startDate ?: LocalDate.now(),
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
        startDate = request.startDate ?: LocalDate.now(),
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
      programServiceCode.startsWith("IND_") -> activityCategories.find { it.code == "SAA_INDUSTRIES" }

      // Prison jobs
      programServiceCode.startsWith("SER_") -> activityCategories.find { it.code == "SAA_PRISON_JOBS" }
      programServiceCode.startsWith("KITCHEN") -> activityCategories.find { it.code == "SAA_PRISON_JOBS" }
      programServiceCode.startsWith("CLNR") -> activityCategories.find { it.code == "SAA_PRISON_JOBS" }
      programServiceCode.startsWith("FG") -> activityCategories.find { it.code == "SAA_PRISON_JOBS" }
      programServiceCode.startsWith("LIBRARY") -> activityCategories.find { it.code == "SAA_PRISON_JOBS" }
      programServiceCode.startsWith("WORKS") -> activityCategories.find { it.code == "SAA_PRISON_JOBS" }
      programServiceCode.startsWith("RECYCLE") -> activityCategories.find { it.code == "SAA_PRISON_JOBS" }

      // Education
      programServiceCode.startsWith("EDU") -> activityCategories.find { it.code == "SAA_EDUCATION" }
      programServiceCode.startsWith("CORECLASS") -> activityCategories.find { it.code == "SAA_EDUCATION" }
      programServiceCode.startsWith("SKILLS") -> activityCategories.find { it.code == "SAA_EDUCATION" }
      programServiceCode.startsWith("KEY_SKILLS") -> activityCategories.find { it.code == "SAA_EDUCATION" }

      // Not in work
      programServiceCode.startsWith("UNEMP") -> activityCategories.find { it.code == "SAA_NOT_IN_WORK" }
      programServiceCode.startsWith("OTH_UNE") -> activityCategories.find { it.code == "SAA_NOT_IN_WORK" }

      // Interventions/courses
      programServiceCode.startsWith("INT_") -> activityCategories.find { it.code == "SAA_INTERVENTIONS" }
      programServiceCode.startsWith("GROUP") -> activityCategories.find { it.code == "SAA_INTERVENTIONS" }
      programServiceCode.startsWith("ABUSE") -> activityCategories.find { it.code == "SAA_INTERVENTIONS" }

      // Sports and fitness
      programServiceCode.startsWith("PE") -> activityCategories.find { it.code == "SAA_GYM_SPORTS_FITNESS" }
      programServiceCode.startsWith("SPORT") -> activityCategories.find { it.code == "SAA_GYM_SPORTS_FITNESS" }
      programServiceCode.startsWith("HEALTH") -> activityCategories.find { it.code == "SAA_GYM_SPORTS_FITNESS" }
      programServiceCode.startsWith("T2PER") -> activityCategories.find { it.code == "SAA_GYM_SPORTS_FITNESS" }
      programServiceCode.startsWith("T2HCW") -> activityCategories.find { it.code == "SAA_GYM_SPORTS_FITNESS" }
      programServiceCode.startsWith("OTH_PER") -> activityCategories.find { it.code == "SAA_GYM_SPORTS_FITNESS" }

      // Faith and spirituality
      programServiceCode.startsWith("CHAP") -> activityCategories.find { it.code == "SAA_FAITH_SPIRITUALITY" }
      programServiceCode.startsWith("T2CFA") -> activityCategories.find { it.code == "SAA_FAITH_SPIRITUALITY" }
      programServiceCode.startsWith("OTH_CFR") -> activityCategories.find { it.code == "SAA_FAITH_SPIRITUALITY" }

      // Induction/guidance
      programServiceCode.startsWith("INDUCTION") -> activityCategories.find { it.code == "SAA_INDUCTION" }
      programServiceCode.startsWith("IAG") -> activityCategories.find { it.code == "SAA_INDUCTION" }
      programServiceCode.startsWith("SAFE") -> activityCategories.find { it.code == "SAA_INDUCTION" }

      // Everything else is Other
      else -> activityCategories.find { it.code == "SAA_OTHER" }
    }
    return category!!
  }

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
