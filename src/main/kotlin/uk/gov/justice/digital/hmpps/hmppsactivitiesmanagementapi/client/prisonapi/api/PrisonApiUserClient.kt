package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CaseLoad

@Service
class PrisonApiUserClient(private val prisonApiUserWebClient: WebClient) : PrisonApiClient(prisonApiUserWebClient) {
  fun getUserCaseLoads(allCaseLoads: Boolean = false): Mono<List<CaseLoad>> {
    return prisonApiUserWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/users/me/caseLoads")
          .queryParam("allCaseloads", allCaseLoads)
          .build()
      }
      .retrieve()
      .bodyToMono(typeReference<List<CaseLoad>>())
  }
}
