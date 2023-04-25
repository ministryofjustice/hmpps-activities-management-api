package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import wiremock.com.fasterxml.jackson.databind.ObjectMapper

class CaseNotesApiMockServer : WireMockServer(8444) {

  private val mapper = ObjectMapper()

  fun stubGetCaseNote(prisonerNumber: String, caseNoteId: Long) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/case-notes/$prisonerNumber/$caseNoteId"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("casenotesapi/case-note-$caseNoteId.json")
            .withStatus(200),
        ),
    )
  }
}
