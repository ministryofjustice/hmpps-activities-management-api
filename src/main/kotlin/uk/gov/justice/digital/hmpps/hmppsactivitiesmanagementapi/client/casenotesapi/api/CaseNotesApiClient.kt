package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.NewCaseNote

const val CASELOAD_ID_HEADER: String = "CaseloadId"
const val CASELOAD_ID_ALL: String = "***"

@Service
class CaseNotesApiClient(@Qualifier("caseNotesApiWebClient") private val webClient: WebClient) {

  fun postCaseNote(prisonCode: String, prisonerNumber: String, caseNote: String, type: CaseNoteType, subType: CaseNoteSubType, caseNotePrefix: String? = null): CaseNote {
    val newCaseNote = NewCaseNote(prisonCode, type.name, subType.name, null, if (caseNotePrefix != null) "$caseNotePrefix\n\n$caseNote".take(4000) else caseNote)
    return webClient.post()
      .uri("/case-notes/{offenderNo}", prisonerNumber)
      .bodyValue(newCaseNote)
      .retrieve()
      .bodyToMono(CaseNote::class.java)
      .block()!!
  }

  fun getCaseNote(prisonerNumber: String, caseNoteId: Long): CaseNote = webClient.get()
    .uri("/case-notes/{offenderNo}/{caseNoteId}", prisonerNumber, caseNoteId)
    .retrieve()
    .bodyToMono(CaseNote::class.java)
    .block()!!

  fun getCaseNoteUUID(prisonerNumber: String, caseNoteId: Long): CaseNote = webClient.get()
    .uri("/case-notes/{offenderNo}/{caseNoteId}", prisonerNumber, caseNoteId)
    .header(CASELOAD_ID_HEADER, CASELOAD_ID_ALL)
    .retrieve()
    .bodyToMono(CaseNote::class.java)
    .block()!!
}

enum class CaseNoteType(val description: String) {
  GEN("General"),
  NEG("Negative behaviour"),
}

enum class CaseNoteSubType(val description: String) {
  HIS("History sheet entry"),
  NEG_GEN("Negative general"),
  IEP_WARN("Incentive warning"),
}
