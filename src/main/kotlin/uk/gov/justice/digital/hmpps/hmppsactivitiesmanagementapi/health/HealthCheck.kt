package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.health

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.hmpps.kotlin.health.HealthPingCheck

@Component
class CaseNotesApiHealth(caseNotesApiHealthWebClient: WebClient) : HealthPingCheck(caseNotesApiHealthWebClient)

@Component
class IncentivesApiHealth(incentivesApiHealthWebClient: WebClient) : HealthPingCheck(incentivesApiHealthWebClient)

@Component
class NonAssociationsApiHealth(nonAssociationsApiHealthWebClient: WebClient) : HealthPingCheck(nonAssociationsApiHealthWebClient)

@Component
class PrisonApiHealth(prisonApiHealthWebClient: WebClient) : HealthPingCheck(prisonApiHealthWebClient)

@Component
class PrisonerSearchApiHealth(prisonerSearchApiHealthWebClient: WebClient) : HealthPingCheck(prisonerSearchApiHealthWebClient)
