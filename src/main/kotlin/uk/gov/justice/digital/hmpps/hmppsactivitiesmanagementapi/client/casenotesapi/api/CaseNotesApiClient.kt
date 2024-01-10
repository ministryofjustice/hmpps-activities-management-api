package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.NewCaseNote

@Service
class CaseNotesApiClient(@Qualifier("caseNotesApiWebClient") private val webClient: WebClient) {

  fun postCaseNote(prisonCode: String, prisonerNumber: String, caseNote: String, type: CaseNoteType, subType: CaseNoteSubType): CaseNote {
    val newCaseNote = NewCaseNote(prisonCode, type.code, subType.code, null, caseNote)
    return webClient.post()
      .uri("/case-notes/{offenderNo}", prisonerNumber)
      .bodyValue(newCaseNote)
      .retrieve()
      .bodyToMono(CaseNote::class.java)
      .block()!!
  }

  fun getCaseNote(prisonerNumber: String, caseNoteId: Long): CaseNote {
    return webClient.get()
      .uri("/case-notes/{offenderNo}/{caseNoteId}", prisonerNumber, caseNoteId)
      .retrieve()
      .bodyToMono(CaseNote::class.java)
      .block()!!
  }
}

enum class CaseNoteType(val code: String) {
  GENERAL("GEN"),
  NEGATIVE_BEHAVIOUR("NEG"),
  ;

  companion object {
    fun get(code: String) = CaseNoteType.entries.find { it.code == code }
      ?: throw NullPointerException("No CaseNoteType found with code: $code")
  }
}

enum class CaseNoteSubType(val code: String) {
  OFFENDER_SUPERVISOR_ENTRY("OSE"),
  NEGATIVE_GENERAL("NEG_GEN"),
  INCENTIVE_WARNING("IEP_WARN"),
  ;

  companion object {
    fun get(code: String) = CaseNoteSubType.entries.find { it.code == code }
      ?: throw NullPointerException("No CaseNoteSubType found with code: $code")
  }
}
