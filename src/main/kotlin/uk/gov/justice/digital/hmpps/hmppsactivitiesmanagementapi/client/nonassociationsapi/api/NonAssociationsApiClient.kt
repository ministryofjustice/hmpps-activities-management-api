package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api

import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.util.context.Context
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.RetryApiService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.PrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference

@Service
class NonAssociationsApiClient(
  private val nonAssociationsApiWebClient: WebClient,
  retryApiService: RetryApiService,
  @Value("\${non-associations.api.retry.max-retries:2}") private val maxRetryAttempts: Long = 2,
  @Value("\${non-associations.api.retry.backoff-millis:250}") private val backoffMillis: Long = 250,
) {
  private val backoffSpec = retryApiService.getBackoffSpec(maxRetryAttempts, backoffMillis)

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getOffenderNonAssociations(prisonerNumber: String): List<PrisonerNonAssociation> = nonAssociationsApiWebClient.get()
    .uri("/prisoner/{prisonerNumber}/non-associations", prisonerNumber)
    .retrieve()
    .bodyToMono(PrisonerNonAssociations::class.java)
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "non-associations-api", "path", "/prisoner/{prisonerNumber}/non-associations")))
    .block()?.nonAssociations ?: emptyList()

  suspend fun getNonAssociationsInvolving(prisonCode: String, prisonerNumbers: List<String>): List<NonAssociation>? {
    if (prisonerNumbers.isEmpty()) return emptyList()

    return runCatching {
      nonAssociationsApiWebClient.post()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/non-associations/involving")
            .queryParam("prisonId", prisonCode)
            .build()
        }
        .bodyValue(prisonerNumbers)
        .retrieve()
        .bodyToMono(typeReference<List<NonAssociation>>())
        .retryWhen(backoffSpec.withRetryContext(Context.of("api", "non-associations-api", "path", "/non-associations/involving")))
        .awaitSingle()
    }.onFailure {
      log.warn("Failed to retrieve non-associations for $prisonCode and involving prisoner numbers $prisonerNumbers", it)
    }.getOrNull()
  }
}
