package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.incentivesapi.api.IncentivesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PlannedSuspension
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SlotTimes
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.ActivityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.consolidateMatchingSlots
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisPayRate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.NomisScheduleRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AllocationMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.ActivityCategoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventOrganiserRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventTierRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.findByCodeOrThrowIllegalArgument
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService.Companion.getSlotForDayAndTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

const val MIGRATION_USER = "MIGRATION"
const val TIER2_IN_CELL_ACTIVITY = "T2ICA"
const val TIER2_STRUCTURED_IN_CELL = "T2SOC"
const val ON_WING_LOCATION = "WOW"
const val DEFAULT_RISK_LEVEL = "low"
const val MAX_ACTIVITY_SUMMARY_SIZE = 50
const val RISLEY_PRISON_CODE = "RSI"

@Service
@Transactional(readOnly = true)
class MigrateActivityService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val activityRepository: ActivityRepository,
  private val prisonRegimeService: PrisonRegimeService,
  private val activityScheduleRepository: ActivityScheduleRepository,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val incentivesApiClient: IncentivesApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val eventTierRepository: EventTierRepository,
  private val activityCategoryRepository: ActivityCategoryRepository,
  private val prisonPayBandRepository: PrisonPayBandRepository,
  private val eventOrganiserRepository: EventOrganiserRepository,
  private val transactionHandler: TransactionHandler,
  private val outboundEventsService: OutboundEventsService,
  private val mapper: ObjectMapper? = null,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)

    fun NomisScheduleRule.usesPrisonRegimeTime(slotStartTime: LocalTime, slotEndTime: LocalTime): Boolean =
      this.startTime == slotStartTime && this.endTime == slotEndTime

    fun NomisScheduleRule.daysOfWeek(): Set<DayOfWeek> =
      setOfNotNull(
        DayOfWeek.MONDAY.takeIf { monday },
        DayOfWeek.TUESDAY.takeIf { tuesday },
        DayOfWeek.WEDNESDAY.takeIf { wednesday },
        DayOfWeek.THURSDAY.takeIf { thursday },
        DayOfWeek.FRIDAY.takeIf { friday },
        DayOfWeek.SATURDAY.takeIf { saturday },
        DayOfWeek.SUNDAY.takeIf { sunday },
      )
  }

  val cohortNames = mapOf(RISLEY_PRISON_CODE to "group").withDefault { "group" }

  @Transactional
  fun migrateActivity(request: ActivityMigrateRequest): ActivityMigrateResponse {
    // Check the prison is rolled out for activities
    if (!rolloutPrisonService.getByPrisonCode(request.prisonCode).activitiesRolledOut) {
      throw ValidationException("The requested prison ${request.prisonCode} is not rolled-out for activities")
    }

    // Get the incentive levels for this prison
    val prisonIncentiveLevels = incentivesApiClient.getIncentiveLevelsCached(request.prisonCode)
    if (prisonIncentiveLevels.isEmpty()) {
      throw ValidationException("No incentive levels found for the requested prison ${request.prisonCode}")
    }

    mapper?.let {
      log.info(
        it.writeValueAsString(
          Pair(request, prisonIncentiveLevels),
        ),
      )
    }

    return transactionHandler.newSpringTransaction {
      // Get the pay bands/aliases configured for this prison
      val payBands = prisonPayBandRepository.findByPrisonCode(request.prisonCode)

      val activities = buildSingleActivity(request)

      // Add the pay rates for paid activities - either 1 or 2 activities created
      activities.forEach { activity ->
        if (activity.isPaid()) {
          request.payRates.forEach {
            val payBand = payBands.find { pb -> pb.nomisPayBand.toString() == it.nomisPayBand }
              ?: logAndThrowValidationException("Failed to migrate activity ${request.description}. No prison pay band for Nomis pay band ${it.nomisPayBand}")

            val iep = prisonIncentiveLevels.find { iep -> iep.levelCode == it.incentiveLevel && iep.active }
              ?: logAndThrowValidationException("Failed to migrate activity ${request.description}. Activity incentive level ${it.incentiveLevel} is not active in this prison")

            activity.addPay(it.incentiveLevel, iep.levelName, payBand, it.rate, null, null, null)
          }
        }
      }

      // Save the activity, schedule, slots and optional pay rates and obtain the activities
      val (activity, splitRegimeActivity) = activityRepository.saveAllAndFlush(activities).let {
        it.first() to it.getOrNull(1)
      }

      log.info("Migrated 1-2-1 activity ${request.description} - ID ${activity.activityId}")

      activity to splitRegimeActivity
    }
      .also { (single, split) ->
        // Send activity schedule created events
        outboundEventsService.send(OutboundEvent.ACTIVITY_SCHEDULE_CREATED, single.schedules().first().activityScheduleId)
        split?.let { outboundEventsService.send(OutboundEvent.ACTIVITY_SCHEDULE_CREATED, it.schedules().first().activityScheduleId) }
      }
      .let { (single, split) -> ActivityMigrateResponse(request.prisonCode, single.activityId, split?.activityId) }
  }

  fun buildSingleActivity(request: ActivityMigrateRequest): List<Activity> {
    val prisonRegimes = prisonRegimeService.getPrisonRegimesByDaysOfWeek(agencyId = request.prisonCode)
    log.info("Migrating activity ${request.description} on a 1-2-1 basis")
    var usePrisonRegimeTimeForActivity = true
    val activity = buildActivityEntity(request)
    request.scheduleRules.consolidateMatchingScheduleSlots().forEach { scheduleRule ->
      val daysOfWeek = getRequestDaysOfWeek(scheduleRule)
      val regimeTimeSlot = scheduleRule.timeSlot ?: prisonRegimes.getSlotForDayAndTime(day = daysOfWeek.first(), time = scheduleRule.startTime)

      if (usePrisonRegimeTimeForActivity) {
        val prisonRegime = scheduleRule.getPrisonRegime(prisonCode = request.prisonCode, timeSlot = regimeTimeSlot)

        usePrisonRegimeTimeForActivity = prisonRegime?.let {
          scheduleRule.usesPrisonRegimeTime(
            slotStartTime = it.first,
            slotEndTime = it.second,
          )
        } ?: false
      }

      activity.schedules().first().addSlot(
        weekNumber = 1,
        slotTimes = Pair(scheduleRule.startTime, scheduleRule.endTime),
        daysOfWeek = daysOfWeek,
        timeSlot = regimeTimeSlot,
      )
    }

    activity.schedules().first().usePrisonRegimeTime = usePrisonRegimeTimeForActivity

    return listOf(activity)
  }

  private fun NomisScheduleRule.getPrisonRegime(prisonCode: String, timeSlot: TimeSlot): SlotTimes? =
    prisonRegimeService.getSlotTimesForTimeSlot(
      prisonCode = prisonCode,
      timeSlot = timeSlot,
      daysOfWeek = this.daysOfWeek(),
    )

  private fun buildActivityEntity(
    request: ActivityMigrateRequest,
    splitRegime: Boolean = false,
    scheduledWeeks: Int = 1,
    cohort: Int? = null,
  ): Activity {
    val tomorrow = LocalDate.now().plusDays(1)

    // For tier two activities we need a default value for the organiser
    val defaultOrganiser = eventOrganiserRepository.findByCodeOrThrowIllegalArgument("OTHER")

    return Activity(
      prisonCode = request.prisonCode,
      activityCategory = mapProgramToCategory(request.programServiceCode),
      activityTier = mapProgramToTier(request.programServiceCode),
      attendanceRequired = true,
      summary = makeNameWithCohortLabel(splitRegime, request.prisonCode, request.description, cohort),
      description = makeNameWithCohortLabel(splitRegime, request.prisonCode, request.description, cohort),
      inCell = (request.internalLocationId == null && !request.outsideWork) ||
        request.programServiceCode == TIER2_IN_CELL_ACTIVITY ||
        request.programServiceCode == TIER2_STRUCTURED_IN_CELL,
      onWing = request.internalLocationCode?.contains(ON_WING_LOCATION) ?: false,
      outsideWork = request.outsideWork,
      startDate = tomorrow,
      riskLevel = DEFAULT_RISK_LEVEL,
      createdTime = LocalDateTime.now(),
      createdBy = MIGRATION_USER,
      isPaid = isPaid(request.payRates),
    ).apply {
      endDate = request.endDate
    }.apply {
      organiser = if (this.activityTier.isTierTwo()) defaultOrganiser else null
    }.apply {
      addSchedule(
        description = this.summary,
        internalLocation = request.internalLocationId?.let { prisonApiClient.getLocation(it).block() },
        capacity = if (request.capacity == 0) 1 else request.capacity,
        startDate = this.startDate,
        endDate = this.endDate,
        runsOnBankHoliday = request.runsOnBankHoliday,
        scheduleWeeks = scheduledWeeks,
      )
    }
  }

  fun isPaid(payRates: List<NomisPayRate>?) = !payRates.isNullOrEmpty()

  fun makeNameWithCohortLabel(splitRegime: Boolean, prisonCode: String, description: String, cohort: Int? = null): String {
    if (!splitRegime) {
      return description
    }

    // Remove the SPLIT label from activity descriptions
    val newDescription = description.replace(" SPLIT", "", true)

    // Add the cohort label e.g. group n, tranche n, cohort n
    val cohortLabel = cohortNames.getValue(prisonCode)
    val labelSize = cohortLabel.length + 3

    return if ((newDescription.trim().length + labelSize) <= MAX_ACTIVITY_SUMMARY_SIZE) {
      "${newDescription.trim()} $cohortLabel $cohort"
    } else {
      "${newDescription.trim().slice(IntRange(0, (MAX_ACTIVITY_SUMMARY_SIZE - labelSize)))} $cohortLabel $cohort"
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
      programServiceCode.startsWith("SAFE") -> activityCategories.isPrisonJobs()

      // Education
      programServiceCode.startsWith("EDU") -> activityCategories.isEducation()
      programServiceCode.startsWith("CORECLASS") -> activityCategories.isEducation()
      programServiceCode.startsWith("SKILLS") -> activityCategories.isEducation()
      programServiceCode.startsWith("KEY_SKILLS") -> activityCategories.isEducation()
      programServiceCode.startsWith("PE_TYPE1") -> activityCategories.isEducation() // Must precede the mapping for "PE" to gym below

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

      // Everything else is Other
      else -> activityCategories.isOther()
    }

    return category ?: throw ValidationException("Could not map $programServiceCode to a category")
  }

  // Helper functions - categories
  fun List<ActivityCategory>.isIndustries() = this.find { it.code == "SAA_INDUSTRIES" }
  fun List<ActivityCategory>.isPrisonJobs() = this.find { it.code == "SAA_PRISON_JOBS" }
  fun List<ActivityCategory>.isEducation() = this.find { it.code == "SAA_EDUCATION" }
  fun List<ActivityCategory>.isNotInWork() = this.find { it.code == "SAA_NOT_IN_WORK" }
  fun List<ActivityCategory>.isInterventions() = this.find { it.code == "SAA_INTERVENTIONS" }
  fun List<ActivityCategory>.isInduction() = this.find { it.code == "SAA_INDUCTION" }
  fun List<ActivityCategory>.isGymSportsFitness() = this.find { it.code == "SAA_GYM_SPORTS_FITNESS" }
  fun List<ActivityCategory>.isFaithSpirituality() = this.find { it.code == "SAA_FAITH_SPIRITUALITY" }
  fun List<ActivityCategory>.isOther() = this.find { it.code == "SAA_OTHER" }

  // Helper functions - tiers
  fun List<EventTier>.isTierOne() = this.find { it.code == "TIER_1" }
  fun List<EventTier>.isTierTwo() = this.find { it.code == "TIER_2" }
  fun List<EventTier>.isFoundation() = this.find { it.code == "FOUNDATION" }

  fun mapProgramToTier(programServiceCode: String): EventTier {
    val tiers = eventTierRepository.findAll()
    val tier = when {
      // Prison industries
      programServiceCode.startsWith("IND_") -> tiers.isTierOne()

      // Prison jobs
      programServiceCode.startsWith("SER_") -> tiers.isTierOne()
      programServiceCode.startsWith("KITCHEN") -> tiers.isTierOne()
      programServiceCode.startsWith("CLNR") -> tiers.isTierOne()
      programServiceCode.startsWith("FG") -> tiers.isTierOne()
      programServiceCode.startsWith("LIBRARY") -> tiers.isTierOne()
      programServiceCode.startsWith("WORKS") -> tiers.isTierOne()
      programServiceCode.startsWith("RECYCLE") -> tiers.isTierOne()
      programServiceCode.startsWith("OTHOCC") -> tiers.isTierOne()

      // Education
      programServiceCode.startsWith("EDU") -> tiers.isTierOne()
      programServiceCode.startsWith("CORECLASS") -> tiers.isTierOne()
      programServiceCode.startsWith("SKILLS") -> tiers.isTierOne()
      programServiceCode.startsWith("KEY_SKILLS") -> tiers.isTierOne()

      // Not in work
      programServiceCode.startsWith("UNEMP") -> tiers.isFoundation()
      programServiceCode.startsWith("OTH_UNE") -> tiers.isFoundation()

      // Interventions/courses
      programServiceCode.startsWith("INT_") -> tiers.isTierOne()
      programServiceCode.startsWith("GROUP") -> tiers.isTierOne()
      programServiceCode.startsWith("ABUSE") -> tiers.isTierOne()

      // Sports and fitness & other T2 services
      programServiceCode.startsWith("PE_TYPE1") -> tiers.isTierOne()
      programServiceCode.startsWith("HEALTH") -> tiers.isTierOne()
      programServiceCode.startsWith("OTH_PER") -> tiers.isTierOne()

      // Specific tier 2 services
      programServiceCode.startsWith("T2") -> tiers.isTierTwo()

      // Faith and spirituality
      programServiceCode.startsWith("CHAP") -> tiers.isTierOne()
      programServiceCode.startsWith("OTH_CFR") -> tiers.isTierOne()

      // Induction/guidance
      programServiceCode.startsWith("INDUCTION") -> tiers.isTierOne()
      programServiceCode.startsWith("IAG") -> tiers.isTierOne()
      programServiceCode.startsWith("SAFE") -> tiers.isTierOne()
      programServiceCode.startsWith("ASSESS") -> tiers.isTierOne()

      // Other miscellaneous
      programServiceCode.startsWith("OTH_DOM") -> tiers.isFoundation()
      programServiceCode.startsWith("OTRESS") -> tiers.isTierOne()
      programServiceCode.startsWith("VIDEO") -> tiers.isFoundation()
      programServiceCode.startsWith("OTH_RCM") -> tiers.isTierOne()
      programServiceCode.startsWith("OTH_RTL") -> tiers.isTierOne()
      programServiceCode.startsWith("OTH_RSM") -> tiers.isTierOne()
      programServiceCode.startsWith("OTH_SEG") -> tiers.isFoundation()
      programServiceCode.startsWith("OTH_INT") -> tiers.isFoundation()
      programServiceCode.startsWith("OTH_HLT") -> tiers.isTierOne()
      programServiceCode.startsWith("OTH_ICA") -> tiers.isTierOne()
      programServiceCode.startsWith("OTH_HCP") -> tiers.isTierOne()
      programServiceCode.startsWith("OTH_IAG") -> tiers.isTierOne()
      programServiceCode.startsWith("OTH_VLA") -> tiers.isFoundation()

      // Association
      programServiceCode.startsWith("ASSOC") -> tiers.isFoundation()

      programServiceCode.startsWith("VIDEO") -> tiers.isFoundation()

      // Everything else is tier 2 by default
      else -> tiers.isTierTwo()
    }

    return tier ?: throw ValidationException("Could not map $programServiceCode to a tier")
  }

  @Transactional
  fun migrateAllocation(request: AllocationMigrateRequest): AllocationMigrateResponse {
    if (!rolloutPrisonService.getByPrisonCode(request.prisonCode).activitiesRolledOut) {
      logAndThrowValidationException("Prison ${request.prisonCode} is not rolled out for activities")
    }

    return transactionHandler.newSpringTransaction {
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

      val prisonerResults = prisonerSearchApiClient.findByPrisonerNumbers(listOf(request.prisonerNumber))
      if (prisonerResults.isEmpty()) {
        logAndThrowValidationException("Allocation failed ${request.prisonerNumber}. Prisoner not found in prisoner search.")
      }

      val prisoner = prisonerResults.first()
      if (prisoner.prisonId != request.prisonCode || prisoner.status?.contains("INACTIVE") == true) {
        logAndThrowValidationException("Allocation failed ${request.prisonerNumber}. Prisoner not in ${request.prisonCode} or INACTIVE")
      }

      // Get the pay bands configured for this prison
      val payBands = prisonPayBandRepository.findByPrisonCode(request.prisonCode)

      // Set the value of the pay band for this allocation
      val prisonPayBand = when {
        request.nomisPayBand.isNullOrEmpty() && activity.isPaid() -> {
          getLowestRatePayBandForActivity(payBands, activity)
        }
        !request.nomisPayBand.isNullOrEmpty() && activity.isPaid() -> {
          // The requested pay band must be configured for this prison
          payBands.find { "${it.nomisPayBand}" == request.nomisPayBand }
            ?: logAndThrowValidationException("Allocation failed ${request.prisonerNumber}. Nomis pay band ${request.nomisPayBand} is not configured for ${request.prisonCode}")
        }
        else -> {
          null
        }
      }

      // Non-null pay bands should exist on a pay rate defined for this activity
      if (prisonPayBand != null) {
        activity.activityPay().find { "${it.payBand.nomisPayBand}" == "${prisonPayBand.nomisPayBand}" }
          ?: logAndThrowValidationException("Allocation failed ${request.prisonerNumber}. Nomis pay band ${prisonPayBand.nomisPayBand} is not on a pay rate for ${activity.activityId} ${activity.description}")
      }

      schedule.allocatePrisoner(
        prisonerNumber = request.prisonerNumber.toPrisonerNumber(),
        payBand = prisonPayBand,
        bookingId = prisoner.bookingId?.toLong() ?: 0L,
        startDate = if (request.startDate.isAfter(tomorrow)) request.startDate else tomorrow,
        endDate = request.endDate,
        exclusions = request.exclusions?.consolidateMatchingSlots(),
        allocatedBy = MIGRATION_USER,
      ).apply {
        if (request.suspendedFlag) {
          log.info("SUSPENDED prisoner ${request.prisonerNumber} being allocated to ${activity.activityId} ${activity.summary} as PENDING with a planned suspension starting immediately")
          addPlannedSuspension(
            PlannedSuspension(
              allocation = this,
              plannedStartDate = startDate,
              plannedBy = MIGRATION_USER,
            ),
          )
        }
      }

      val savedSchedule = activityScheduleRepository.saveAndFlush(schedule)

      val allocation = savedSchedule.allocations(excludeEnded = true)
        .find { it.prisonerNumber == request.prisonerNumber }
        ?: logAndThrowValidationException("Allocation failed ${request.prisonerNumber}. Could not re-read the saved allocation")

      log.info("Allocated ${request.prisonerNumber} to ${activity.activityId} ${activity.summary} with allocation ID ${allocation.allocationId}")

      allocation.allocationId
    }.let { allocationId ->
      outboundEventsService.send(OutboundEvent.PRISONER_ALLOCATED, allocationId)
      AllocationMigrateResponse(request.activityId, allocationId)
    }
  }

  private fun logAndThrowValidationException(msg: String): Nothing {
    log.error(msg)
    throw ValidationException(msg)
  }

  private fun getLowestRatePayBandForActivity(
    prisonPayBands: List<PrisonPayBand>,
    activity: Activity,
  ): PrisonPayBand? {
    val sortedActivityRates = activity.activityPay().sortedBy { it.rate }
    return if (sortedActivityRates.isEmpty()) {
      null
    } else {
      prisonPayBands.find { it.nomisPayBand == sortedActivityRates.first().payBand.nomisPayBand }
    }
  }

  @PreAuthorize("hasAnyRole('NOMIS_ACTIVITIES')")
  @Transactional
  fun deleteActivityCascade(prisonCode: String, activityId: Long) {
    log.info("Delete cascade activity ID $activityId")
    val activity = activityRepository.findByActivityIdAndPrisonCode(activityId, prisonCode)
      ?: logAndThrowValidationException("Failed to delete. Activity $activityId was not found at prison $prisonCode")
    activityRepository.delete(activity)
  }

  private fun List<NomisScheduleRule>.consolidateMatchingScheduleSlots() =
    groupBy { Triple(it.startTime, it.endTime, it.timeSlot) }
      .let { rulesBySlotTimes ->
        rulesBySlotTimes.map { (slotTimes, groupedRules) ->
          NomisScheduleRule(
            startTime = slotTimes.first,
            endTime = slotTimes.second,
            monday = groupedRules.any { it.monday },
            tuesday = groupedRules.any { it.tuesday },
            wednesday = groupedRules.any { it.wednesday },
            thursday = groupedRules.any { it.thursday },
            friday = groupedRules.any { it.friday },
            saturday = groupedRules.any { it.saturday },
            sunday = groupedRules.any { it.sunday },
            timeSlot = slotTimes.third,
          )
        }
      }
}
