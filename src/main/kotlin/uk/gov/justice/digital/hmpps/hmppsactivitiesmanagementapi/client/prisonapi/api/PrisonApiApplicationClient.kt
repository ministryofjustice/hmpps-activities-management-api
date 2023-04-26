package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

/**
 * To be used for prison-api calls which are not on the back of a web request to a controller e.g. we receive an event
 * which in turn fires off a call to prison-api.
 */
@Service
class PrisonApiApplicationClient(prisonApiAppWebClient: WebClient) : PrisonApiClient(prisonApiAppWebClient)
