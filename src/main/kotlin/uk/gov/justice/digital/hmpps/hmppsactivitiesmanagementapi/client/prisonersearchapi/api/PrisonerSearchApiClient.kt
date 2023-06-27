package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PagedPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerNumbers

@Service
class PrisonerSearchApiClient(private val prisonerSearchApiWebClient: WebClient) {

  fun getAllPrisonersInPrison(prisonCode: String) = prisonerSearchApiWebClient
    .get()
    .uri("/prison/$prisonCode/prisoners?size=2000")
    .header("Content-Type", "application/json")
    .retrieve()
    .bodyToMono(typeReference<PagedPrisoner>())

  fun findByPrisonerNumbers(prisonerNumbers: List<String>): Mono<List<Prisoner>> {
    if (prisonerNumbers.isEmpty()) return Mono.just(emptyList())
    return prisonerSearchApiWebClient.post()
      .uri("/prisoner-search/prisoner-numbers")
      .bodyValue(PrisonerNumbers(prisonerNumbers))
      .retrieve()
      .bodyToMono(typeReference<List<Prisoner>>())
  }

  fun findByPrisonerNumbersMap(prisonerNumbers: List<String>): Map<String, Prisoner> =
    findByPrisonerNumbers(prisonerNumbers).block()!!.associateBy { it.prisonerNumber }

  suspend fun findByPrisonerNumbersAsync(prisonerNumbers: List<String>): List<Prisoner> {
    if (prisonerNumbers.isEmpty()) return emptyList()
    return prisonerSearchApiWebClient.post()
      .uri("/prisoner-search/prisoner-numbers")
      .bodyValue(PrisonerNumbers(prisonerNumbers))
      .retrieve()
      .awaitBody()
  }
}
