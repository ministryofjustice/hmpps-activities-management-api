package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.incentivesapi.api

import kotlinx.coroutines.runBlocking
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.incentivesapi.model.PrisonIncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.CacheConfiguration

@Service
class IncentivesApiClient(private val incentivesApiWebClient: WebClient) {

  fun getIncentiveLevels(prisonId: String): List<PrisonIncentiveLevel> = runBlocking {
    incentivesApiWebClient.get()
      .uri("/incentive/prison-levels/$prisonId")
      .retrieve()
      .awaitBody()
  }

  @Cacheable(CacheConfiguration.PRISON_INCENTIVE_LEVELS_CACHE_NAME)
  fun getIncentiveLevelsCached(prisonId: String): List<PrisonIncentiveLevel> = getIncentiveLevels(prisonId)
}
