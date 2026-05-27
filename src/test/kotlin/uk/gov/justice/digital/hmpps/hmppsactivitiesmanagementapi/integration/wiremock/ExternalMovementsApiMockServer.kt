package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.fasterxml.jackson.databind.SerializationFeature
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementsRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementsResponse
import java.time.LocalDateTime

class ExternalMovementsApiMockServer : MockServer(8095) {

  private val dateMapper = mapper.copy().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  fun stubGetExternalMovements(prisonCode: String, prisonerNumbers: List<String> = emptyList(), start: LocalDateTime, end: LocalDateTime, response: ExternalMovementsResponse) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/search/prisons/$prisonCode/external-activities"))
        .withRequestBody(equalToJson(dateMapper.writeValueAsString(ExternalMovementsRequest(prisonerNumbers, start, end)), true, true))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(dateMapper.writeValueAsString(response))
            .withStatus(200),
        ),
    )
  }

  fun stubGetExternalMovementsWithConnectionReset(prisonCode: String, prisonerNumbers: List<String> = emptyList(), start: LocalDateTime, end: LocalDateTime, response: ExternalMovementsResponse, numFails: Int = 1) {
    val url = "/search/prisons/$prisonCode/external-activities"

    for (i in 1..numFails) {
      stubFor(
        WireMock.post(WireMock.urlEqualTo(url))
          .withRequestBody(equalToJson(dateMapper.writeValueAsString(ExternalMovementsRequest(prisonerNumbers, start, end)), true, true))
          .inScenario("Network fail")
          .whenScenarioStateIs(if (i == 1) STARTED else "Fail ${i - 1}")
          .willReturn(
            WireMock.aResponse()
              .withFault(Fault.CONNECTION_RESET_BY_PEER),
          )
          .willSetStateTo("Fail $i"),
      )
    }

    stubFor(
      WireMock.post(WireMock.urlEqualTo(url))
        .withRequestBody(equalToJson(dateMapper.writeValueAsString(ExternalMovementsRequest(prisonerNumbers, start, end)), true, true))
        .inScenario("Network fail")
        .whenScenarioStateIs("Fail $numFails")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(dateMapper.writeValueAsString(response))
            .withStatus(200),
        ),
    )
  }
}
