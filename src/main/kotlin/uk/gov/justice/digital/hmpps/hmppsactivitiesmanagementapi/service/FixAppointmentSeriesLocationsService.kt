package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api.LocationsInsidePrisonAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisMappingAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSeriesRepository

@Service
class FixAppointmentSeriesLocationsService(
  private val appointmentSeriesRepository: AppointmentSeriesRepository,
  private val nomisMappingAPIClient: NomisMappingAPIClient,
  private val locationsInsidePrisonAPIClient: LocationsInsidePrisonAPIClient,
  private val transactionHandler: TransactionHandler,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun fixLocations() {
    val ids = appointmentSeriesRepository.findNomisLocationsIds()

    log.info("Found ${ids.size} appointment series locations ids with null DPS Location UUIDS")

    ids.mapNotNull { internalLocationId ->
      log.debug("Internal location id is: $internalLocationId")

      try {
        internalLocationId to nomisMappingAPIClient.getLocationMappingByNomisId(internalLocationId.toLong())
      } catch (e: Exception) {
        log.warn("Failed to find DPS location UUID for Nomis location ID: $internalLocationId", e)
        null
      }
    }.mapNotNull { (internalLocationId, dpsLocationMapping) ->
      log.info("DPS Location location UUID found for ${dpsLocationMapping?.nomisLocationId} - ${dpsLocationMapping?.dpsLocationId}")

      try {
        internalLocationId to locationsInsidePrisonAPIClient.getLocationById(dpsLocationMapping?.dpsLocationId!!)
      } catch (e: Exception) {
        log.warn("Failed to find DPS location UUID for Nomis location ID: ${dpsLocationMapping?.dpsLocationId}", e)
        null
      }
    }.forEach { (internalLocationId, dpsLocation) ->
      if (dpsLocation.localName == null) {
        log.warn("No local name for: ${dpsLocation.id} ${dpsLocation.prisonId} ${dpsLocation.code}")
      }

      transactionHandler.newSpringTransaction {
        appointmentSeriesRepository.updateLocationDetails(
          internalLocationId,
          dpsLocation.id,
        )
      }
    }

    log.info("Finished running appointment series fix locations job")
  }
}
