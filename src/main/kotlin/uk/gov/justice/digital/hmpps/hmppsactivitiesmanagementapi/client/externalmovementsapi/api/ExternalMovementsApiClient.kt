package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.api

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.util.context.Context
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.RetryApiService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementsRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model.ExternalMovementsResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference
import java.time.LocalDateTime

@Service
class ExternalMovementsApiClient(
  private val externalMovementsApiWebClient: WebClient,
  retryApiService: RetryApiService,
  @Value("\${external-movements.api.retry.max-retries:2}") private val maxRetryAttempts: Long = 2,
  @Value("\${external-movements.api.retry.backoff-millis:250}") private val backoffMillis: Long = 250,
) {
  private val backoffSpec = retryApiService.getBackoffSpec(maxRetryAttempts, backoffMillis)

  suspend fun getExternalMovements(prisonCode: String, prisonerNumbers: Collection<String>, start: LocalDateTime, end: LocalDateTime): ExternalMovementsResponse = externalMovementsApiWebClient.post()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/search/prisons/{prisonCode}/external-activities")
        .build(prisonCode)
    }
    .bodyValue(ExternalMovementsRequest(prisonerNumbers.toList(), start, end))
    .retrieve()
    .bodyToMono(typeReference<ExternalMovementsResponse>())
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "external-movements-api", "path", "/search/prisons/{prisonCode}/external-activities")))
    .awaitSingle()
}
