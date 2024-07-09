package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.HearingsResponse

class ManageAdjudicationsApiMockServer : MockServer(8777) {

  fun getHearings(): List<HearingsResponse> = TODO("implement me")
}
