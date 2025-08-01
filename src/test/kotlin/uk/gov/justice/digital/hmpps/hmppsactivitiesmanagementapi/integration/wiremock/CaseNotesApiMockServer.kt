package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CASELOAD_ID_ALL
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CASELOAD_ID_HEADER
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteSubType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNoteType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.USERNAME_HEADER
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.NewCaseNote
import java.util.UUID

class CaseNotesApiMockServer : MockServer(8444) {

  fun stubGetCaseNote(prisonerNumber: String, caseNoteId: UUID) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/case-notes/$prisonerNumber/$caseNoteId"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withHeader(CASELOAD_ID_HEADER, CASELOAD_ID_ALL)
            .withBodyFile("casenotesapi/case-note-$caseNoteId.json")
            .withStatus(200),
        ),
    )
  }

  fun stubGetCaseNoteUUID(prisonerNumber: String, caseNoteId: Long) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/case-notes/$prisonerNumber/$caseNoteId"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withHeader(CASELOAD_ID_HEADER, CASELOAD_ID_ALL)
            .withBodyFile("casenotesapi/case-note-$caseNoteId.json")
            .withStatus(200),
        ),
    )
  }

  fun stubPostCaseNote(caseNoteId: UUID, prisonCode: String, prisonerNumber: String, caseNote: String, type: CaseNoteType, subType: CaseNoteSubType) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/case-notes/$prisonerNumber"))
        .withRequestBody(
          WireMock.equalToJson(
            mapper.writeValueAsString(NewCaseNote(prisonCode, type.name, subType.name, null, caseNote)),
          ),
        )
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withHeader(CASELOAD_ID_HEADER, CASELOAD_ID_ALL)
            .withHeader(USERNAME_HEADER, "joebloggs")
            .withBodyFile("casenotesapi/case-note-$caseNoteId.json")
            .withStatus(200),
        ),
    )
  }
}
