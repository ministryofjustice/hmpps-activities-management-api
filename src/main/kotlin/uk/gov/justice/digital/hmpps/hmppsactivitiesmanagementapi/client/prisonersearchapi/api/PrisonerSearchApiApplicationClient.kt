package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.RetryApiService

@Service
class PrisonerSearchApiApplicationClient(prisonerSearchApiAppWebClient: WebClient, retryApiService: RetryApiService) : PrisonerSearchApiClient(prisonerSearchApiAppWebClient, retryApiService)
