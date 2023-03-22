package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerNumbers

@Service
class PrisonerSearchApiClient(private val prisonerSearchApiWebClient: WebClient) {

  fun findByPrisonerNumbers(prisonerNumbers: List<String>): Mono<List<Prisoner>> {
    return prisonerSearchApiWebClient.post()
      .uri("/prisoner-search/prisoner-numbers")
      .bodyValue(PrisonerNumbers(prisonerNumbers))
      .retrieve()
      .bodyToMono(typeReference<List<Prisoner>>())
  }

  // Not used (yet)
  suspend fun findByPrisonerNumbersAsync(prisonerNumbers: List<String>): List<Prisoner> =
    prisonerSearchApiWebClient.post()
      .uri("/prisoner-search/prisoner-numbers")
      .bodyValue(PrisonerNumbers(prisonerNumbers))
      .retrieve()
      .awaitBody()
}
