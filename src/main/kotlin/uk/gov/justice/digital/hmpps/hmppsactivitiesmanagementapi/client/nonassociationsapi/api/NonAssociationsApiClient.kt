package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.PrisonerNonAssociations

@Service
class NonAssociationsApiClient(private val nonAssociationsApiWebClient: WebClient) {
  fun getOffenderNonAssociations(prisonerNumber: String): List<PrisonerNonAssociation> {
    return nonAssociationsApiWebClient.get()
      .uri("/prisoner/{prisonerNumber}/non-associations", prisonerNumber)
      .retrieve()
      .bodyToMono(PrisonerNonAssociations::class.java)
      .block()?.nonAssociations ?: emptyList()
  }
}
