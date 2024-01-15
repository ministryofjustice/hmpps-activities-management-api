package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.incentives.api

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.incentives.model.PrisonIncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.typeReference
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.CacheConfiguration

@Service
class IncentivesApiClient(private val incentivesApiWebClient: WebClient) {

  fun getIncentiveLevels(prisonId: String): List<PrisonIncentiveLevel> {
    return incentivesApiWebClient.get()
      .uri("/incentive/prison-levels/$prisonId")
      .retrieve()
      .bodyToMono(typeReference<List<PrisonIncentiveLevel>>())
      .block() ?: emptyList()
  }

  @Cacheable(CacheConfiguration.PRISON_INCENTIVE_LEVELS_CACHE_NAME)
  fun getIncentiveLevelsCached(prisonId: String): List<PrisonIncentiveLevel> =
    getIncentiveLevels(prisonId)
}
