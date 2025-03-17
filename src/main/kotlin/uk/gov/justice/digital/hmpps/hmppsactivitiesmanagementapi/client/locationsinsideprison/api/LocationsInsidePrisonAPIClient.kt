package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto
import java.util.*

@Service
class LocationsInsidePrisonAPIClient(private val locationsInsidePrisonApiWebClient: WebClient) {

  fun getLocationById(id: UUID) = locationsInsidePrisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/locations/{id}")
        .queryParam("formatLocalName", true)
        .build(id)
    }
    .retrieve()
    .bodyToMono(Location::class.java)
    .block()!!

  suspend fun getLocationsWithUsageTypes(prisonCode: String): List<Location> = locationsInsidePrisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/locations/prison/{prisonCode}/non-residential-usage-type")
        .queryParam("formatLocalName", true)
        .build(prisonCode)
    }
    .retrieve()
    .awaitBody()

  suspend fun getLocationsForUsageType(prisonCode: String, usageType: NonResidentialUsageDto.UsageType): List<Location> = locationsInsidePrisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/locations/prison/{prisonCode}/non-residential-usage-type/{usageType}")
        .queryParam("formatLocalName", true)
        .build(prisonCode, usageType)
    }
    .retrieve()
    .awaitBody()
}
