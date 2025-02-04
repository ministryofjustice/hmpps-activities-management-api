package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class PrisonerSearchApiApplicationClient(prisonerSearchApiAppWebClient: WebClient) : PrisonerSearchApiClient(prisonerSearchApiAppWebClient)
