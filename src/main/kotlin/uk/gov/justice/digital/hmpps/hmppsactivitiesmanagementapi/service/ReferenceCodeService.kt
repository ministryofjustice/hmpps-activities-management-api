package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient

@Service
class ReferenceCodeService(
  private val prisonApiClient: PrisonApiClient,
) {
  fun getReasonCodes(eventType: String)
}
