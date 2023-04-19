package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ReferenceCode

@Service
class ReferenceCodeService(
  private val prisonApiClient: PrisonApiClient,
) {
  fun getReferenceCodes(domain: ReferenceCodeDomain): List<ReferenceCode> =
    prisonApiClient.getReferenceCodes(domain.value)

  fun getReferenceCodesMap(domain: ReferenceCodeDomain): Map<String, ReferenceCode> =
    getReferenceCodes(domain).associateBy { it.code }

  fun getScheduleReasons(eventType: ScheduleReasonEventType): List<ReferenceCode> =
    prisonApiClient.getScheduleReasons(eventType.value)

  fun getScheduleReasonsMap(eventType: ScheduleReasonEventType): Map<String, ReferenceCode> =
    getScheduleReasons(eventType).associateBy { it.code }

  fun getScheduleLocations(prisonCode: String, eventType: ScheduleReasonEventType): List<Location> =
    prisonApiClient.getLocationsForTypeUnrestricted(prisonCode, eventType.value).block() ?: emptyList()
}

enum class ReferenceCodeDomain(val value: String) {
  APPOINTMENT_CATEGORY("INT_SCH_RSN"),
}

enum class ScheduleReasonEventType(val value: String) {
  APPOINTMENT("APP"),
}
