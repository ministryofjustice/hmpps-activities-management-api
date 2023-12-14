package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.NewCaseNote

@Service
class CaseNotesApiClient(@Qualifier("caseNotesApiWebClient") private val webClient: WebClient) {

  fun postCaseNote(prisonCode: String, prisonerNumber: String, caseNote: String, type: String, subType: String): CaseNote? {
    val newCaseNote = NewCaseNote(prisonCode, type, subType, null, caseNote)
    return webClient.post()
      .uri("/case-notes/{offenderNo}", prisonerNumber)
      .bodyValue(newCaseNote)
      .retrieve()
      .bodyToMono(CaseNote::class.java)
      .block()
  }

  fun getCaseNote(prisonerNumber: String, caseNoteId: Long?): CaseNote? {
    return webClient.get()
      .uri("/case-notes/{offenderNo}/{caseNoteId}", prisonerNumber, caseNoteId)
      .retrieve()
      .bodyToMono(CaseNote::class.java)
      .block()
  }
}
