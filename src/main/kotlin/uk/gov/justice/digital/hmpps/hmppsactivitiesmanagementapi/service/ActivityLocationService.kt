package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api.LocationsInsidePrisonAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService

@Service
@Transactional(readOnly = true)
class ActivityLocationService(
  private val locationsInsidePrisonAPIClient: LocationsInsidePrisonAPIClient,
  private val rolloutService: RolloutPrisonService,
  private val activityScheduleRepository: ActivityScheduleRepository,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getInvalidActivityLocations() = runBlocking {
    rolloutService.getRolloutPrisons()
      .map { it.prisonCode }
      .sortedBy { it }
      .flatMap {
        log.info("Checking for invalid activities for location $it")

        val validUuids = locationsInsidePrisonAPIClient.getLocationsForUsageType(it, NonResidentialUsageDto.UsageType.PROGRAMMES_ACTIVITIES).map { it.id }.toSet()

        activityScheduleRepository.findInvalidLocationUuids(it)
          .filter { validUuids.contains(it).not() }
          .flatMap {
            log.info("Finding invalid activities for location $it")

            activityScheduleRepository.findByInvalidLocationUuids(it)
          }
      }
  }
}
