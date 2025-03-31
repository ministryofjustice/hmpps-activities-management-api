package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.util.context.Context
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.RetryApiService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference
import java.util.*

@Service
class NomisMappingAPIClient(
  private val nomisMappingApiWebClient: WebClient,
  retryApiService: RetryApiService,
  @Value("\${nomis-mapping.api.retry.max-retries:2}") private val maxRetryAttempts: Long = 2,
  @Value("\${nomis-mapping.api.retry.backoff-millis:250}") private val backoffMillis: Long = 250,
) {
  private val backoffSpec = retryApiService.getBackoffSpec(maxRetryAttempts, backoffMillis)

  fun getLocationMappingByDpsId(dpsLocationId: UUID): NomisDpsLocationMapping? = nomisMappingApiWebClient
    .get()
    .uri("/api/locations/dps/{id}", dpsLocationId)
    .retrieve()
    .bodyToMono(NomisDpsLocationMapping::class.java)
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "nomis-mapping-api", "path", "/api/locations/dps/{id}")))
    .block()!!

  fun getLocationMappingByNomisId(nomisLocationId: Long): NomisDpsLocationMapping? = nomisMappingApiWebClient
    .get()
    .uri("/api/locations/nomis/{id}", nomisLocationId)
    .retrieve()
    .bodyToMono(NomisDpsLocationMapping::class.java)
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "nomis-mapping-api", "path", "/api/locations/nomis/{id}")))
    .block()!!

  suspend fun getLocationMappingsByNomisIds(nomisLocationIds: Set<Long>): List<NomisDpsLocationMapping> {
    if (nomisLocationIds.isEmpty()) return emptyList()
    return nomisMappingApiWebClient.post()
      .uri("/api/locations/nomis")
      .bodyValue(nomisLocationIds)
      .retrieve()
      .bodyToMono(typeReference<List<NomisDpsLocationMapping>>())
      .retryWhen(backoffSpec.withRetryContext(Context.of("api", "nomis-mapping-api", "path", "/api/locations/nomis")))
      .awaitSingle()
  }

  suspend fun getLocationMappingsByDpsIds(dpsLocationIds: Set<UUID>): List<NomisDpsLocationMapping> {
    if (dpsLocationIds.isEmpty()) return emptyList()
    return nomisMappingApiWebClient.post()
      .uri("/api/locations/dps")
      .bodyValue(dpsLocationIds)
      .retrieve()
      .bodyToMono(typeReference<List<NomisDpsLocationMapping>>())
      .retryWhen(backoffSpec.withRetryContext(Context.of("api", "nomis-mapping-api", "path", "/api/locations/dps")))
      .awaitSingle()
  }
}

data class NomisDpsLocationMapping(
  val dpsLocationId: UUID,
  val nomisLocationId: Long,
)
