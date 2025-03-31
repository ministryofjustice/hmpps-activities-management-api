package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.util.context.Context
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.RetryApiService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PagedPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerNumbers

@Service
class PrisonerSearchApiClient(
  private val prisonerSearchApiWebClient: WebClient,
  retryApiService: RetryApiService,
  @Value("\${prisoner-search.api.retry.max-retries:2}") private val maxRetryAttempts: Long = 2,
  @Value("\${prisoner-search.api.retry.backoff-millis:250}") private val backoffMillis: Long = 250,
) {
  private val backoffSpec = retryApiService.getBackoffSpec(maxRetryAttempts, backoffMillis)

  fun getAllPrisonersInPrison(prisonCode: String) = prisonerSearchApiWebClient
    .get()
    .uri("/prison/$prisonCode/prisoners?size=2000")
    .header("Content-Type", "application/json")
    .retrieve()
    .bodyToMono(typeReference<PagedPrisoner>())
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prisoner-search-api", "path", "/prison/{prisonCode}/prisoners")))

  fun findByPrisonerNumbers(prisonerNumbers: List<String>, batchSize: Int = 1000): List<Prisoner> {
    require(batchSize in 1..1000) {
      "Batch size must be between 1 and 1000"
    }
    if (prisonerNumbers.isEmpty()) return emptyList()
    return prisonerNumbers.chunked(batchSize).flatMap {
      prisonerSearchApiWebClient.post()
        .uri("/prisoner-search/prisoner-numbers")
        .bodyValue(PrisonerNumbers(it))
        .retrieve()
        .bodyToMono(typeReference<List<Prisoner>>())
        .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prisoner-search-api", "path", "/prisoner-search/prisoner-numbers")))
        .block() ?: emptyList()
    }
  }

  fun findByPrisonerNumbersMap(prisonerNumbers: List<String>): Map<String, Prisoner> = findByPrisonerNumbers(prisonerNumbers).associateBy { it.prisonerNumber }

  suspend fun findByPrisonerNumbersAsync(prisonerNumbers: List<String>): List<Prisoner> {
    if (prisonerNumbers.isEmpty()) return emptyList()
    return prisonerSearchApiWebClient.post()
      .uri("/prisoner-search/prisoner-numbers")
      .bodyValue(PrisonerNumbers(prisonerNumbers))
      .retrieve()
      .bodyToMono(typeReference<List<Prisoner>>())
      .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prisoner-search-api", "path", "/prisoner-search/prisoner-numbers")))
      .awaitSingle()
  }

  fun findByPrisonerNumber(prisonerNumber: String): Prisoner? = prisonerSearchApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/prisoner/{prisonerNumber}")
        .build(prisonerNumber)
    }
    .retrieve()
    .bodyToMono(typeReference<Prisoner>())
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prisoner-search-api", "path", "/prisoner/{prisonerNumber}")))
    .block()
}
