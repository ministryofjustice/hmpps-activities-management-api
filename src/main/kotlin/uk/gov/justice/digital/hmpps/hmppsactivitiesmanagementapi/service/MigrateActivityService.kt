package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationMigrateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.AllocationMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository

@Service
@Transactional(readOnly = true)
class MigrateActivityService(
  private val rolloutPrisonService: RolloutPrisonService,
  private val activityRepository: ActivityRepository,
  private val activityScheduleRepository: ActivityScheduleRepository,
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

    // Is this a split regime activity? Different paths if so
    // Create the activity with its pay rates
    // Add the schedule and slot times
    // Build a response

    return ActivityMigrateResponse("RSI", 1L, 2L)
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
