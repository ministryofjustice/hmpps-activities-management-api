package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import java.util.UUID

data class LocationRemapChange(
  val objectType: String,
  val objectId: Long,
  val prisonCode: String,
  val name: String,
  val oldDpsLocationId: UUID?,
  val newDpsLocationId: UUID,
  val oldLocationName: String,
  val newLocationName: String,
  val oldNomisLocationId: Long?,
  val newNomisLocationId: Long,
  val description: String? = null,
  val activityId: Long? = null,
  val appointmentSeriesId: Long? = null,
  val appointmentSetId: Long? = null,
  val appointmentType: String? = null,
) {
  companion object {
    private val log = LoggerFactory.getLogger(LocationRemapChange::class.java)
    private val mapper = jacksonObjectMapper()
  }

  fun logCommitted() {
    log.info("Location remap committed {}", mapper.writeValueAsString(this))
  }
}

fun LocationService.getSourceLocationName(mapping: LocationRemap): String = runCatching {
  getDpsLocationDetails(mapping.locationId).let { it.localName?.takeIf(String::isNotBlank) ?: it.code }
}.getOrElse { mapping.locationName ?: "Unavailable" }
