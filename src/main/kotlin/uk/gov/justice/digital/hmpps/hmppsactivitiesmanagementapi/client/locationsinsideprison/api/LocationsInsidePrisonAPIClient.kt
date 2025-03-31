package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.util.context.Context
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.RetryApiService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference
import java.util.*

@Service
class LocationsInsidePrisonAPIClient(
  private val locationsInsidePrisonApiWebClient: WebClient,
  retryApiService: RetryApiService,
  @Value("\${locations-inside-prison.api.retry.max-retries:2}") private val maxRetryAttempts: Long = 2,
  @Value("\${locations-inside-prison.api.retry.backoff-millis:250}") private val backoffMillis: Long = 250,
) {
  private val backoffSpec = retryApiService.getBackoffSpec(maxRetryAttempts, backoffMillis)

  fun getLocationById(id: UUID) = locationsInsidePrisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/locations/{id}")
        .queryParam("formatLocalName", true)
        .build(id)
    }
    .retrieve()
    .bodyToMono(Location::class.java)
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "locations-inside-prison-api", "path", "/locations/{id}")))
    .block()!!

  suspend fun getNonResidentialLocations(prisonCode: String): List<Location> = locationsInsidePrisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/locations/prison/{prisonCode}/non-residential")
        .queryParam("formatLocalName", true)
        .build(prisonCode)
    }
    .retrieve()
    .bodyToMono(typeReference<List<Location>>())
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "locations-inside-prison-api", "path", "/locations/prison/{prisonCode}/non-residential")))
    .awaitSingle()

  suspend fun getLocationsForUsageType(prisonCode: String, usageType: NonResidentialUsageDto.UsageType): List<Location> = locationsInsidePrisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/locations/prison/{prisonCode}/non-residential-usage-type/{usageType}")
        .queryParam("formatLocalName", true)
        .queryParam("filterParents", false)
        .build(prisonCode, usageType)
    }
    .retrieve()
    .bodyToMono(typeReference<List<Location>>())
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "locations-inside-prison-api", "path", "/locations/prison/{prisonCode}/non-residential-usage-type/{usageType}")))
    .awaitSingle()
}
