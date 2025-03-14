package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.util.*

@Service
class NomisMappingAPIClient(private val nomisMappingApiWebClient: WebClient) {

  fun getLocationMappingByDpsId(dpsLocationId: UUID): NomisDpsLocationMapping? = nomisMappingApiWebClient
    .get()
    .uri("/api/locations/dps/{id}", dpsLocationId)
    .retrieve()
    .bodyToMono(NomisDpsLocationMapping::class.java)
    .block()!!

  fun getLocationMappingByNomisId(nomisLocationId: Long): NomisDpsLocationMapping? = nomisMappingApiWebClient
    .get()
    .uri("/api/locations/nomis/{id}", nomisLocationId)
    .retrieve()
    .bodyToMono(NomisDpsLocationMapping::class.java)
    .block()!!

  suspend fun getLocationMappingsByNomisIds(nomisLocationIds: Set<Long>): List<NomisDpsLocationMapping> {
    if (nomisLocationIds.isEmpty()) return emptyList()
    return nomisMappingApiWebClient.post()
      .uri("/api/locations/nomis")
      .bodyValue(nomisLocationIds)
      .retrieve()
      .awaitBody()
  }

  suspend fun getLocationMappingsByDpsIds(dpsLocationIds: Set<UUID>): List<NomisDpsLocationMapping> {
    if (dpsLocationIds.isEmpty()) return emptyList()
    return nomisMappingApiWebClient.post()
      .uri("/api/locations/dps")
      .bodyValue(dpsLocationIds)
      .retrieve()
      .awaitBody()
  }
}

data class NomisDpsLocationMapping(
  val dpsLocationId: UUID,
  val nomisLocationId: Long,
)
