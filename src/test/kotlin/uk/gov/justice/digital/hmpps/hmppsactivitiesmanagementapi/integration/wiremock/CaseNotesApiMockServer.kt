package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteSubType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.NewCaseNote

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

  fun stubPostCaseNote(caseNoteId: Long, prisonCode: String, prisonerNumber: String, caseNote: String, type: CaseNoteType, subType: CaseNoteSubType) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/case-notes/$prisonerNumber"))
        .withRequestBody(
          WireMock.equalToJson(
            mapper.writeValueAsString(NewCaseNote(prisonCode, type.code, subType.code, null, caseNote)),
          ),
        )
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("casenotesapi/case-note-$caseNoteId.json")
            .withStatus(200),
        ),
    )
  }
}
