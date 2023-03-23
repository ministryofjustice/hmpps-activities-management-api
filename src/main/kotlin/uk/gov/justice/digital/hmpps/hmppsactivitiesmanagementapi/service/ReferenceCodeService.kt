package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ReferenceCode

@Service
class ReferenceCodeService(
  private val prisonApiClient: PrisonApiClient,
) {
  fun getReferenceCodes(domain: String): List<ReferenceCode> =
    prisonApiClient.getReferenceCodes(domain)

  fun getAppointmentCategoryReferenceCodes(): List<ReferenceCode> =
    getReferenceCodes("INT_SCH_RSN")

  fun getScheduleReasons(eventType: String): List<ReferenceCode> =
    prisonApiClient.getScheduleReasons(eventType)

  fun getAppointmentScheduleReasons(): List<ReferenceCode> =
    getScheduleReasons("APP")
}
