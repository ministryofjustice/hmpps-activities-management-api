package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.Location
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
}
