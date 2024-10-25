package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriBuilder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.PrisonerNonAssociations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches

@Service
class NonAssociationsApiClient(
  private val nonAssociationsApiWebClient: WebClient,
  features: FeatureSwitches,
) {
  private val nonAssociationEnabled = features.isEnabled(Feature.NON_ASSOCIATIONS_ENABLED)

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getOffenderNonAssociations(prisonerNumber: String): List<PrisonerNonAssociation> {
    return nonAssociationsApiWebClient.get()
      .uri("/prisoner/{prisonerNumber}/non-associations", prisonerNumber)
      .retrieve()
      .bodyToMono(PrisonerNonAssociations::class.java)
      .block()?.nonAssociations ?: emptyList()
  }

  suspend fun getNonAssociationsInvolving(prisonCode: String, prisonerNumbers: List<String>): List<NonAssociation>? {
    if (!nonAssociationEnabled) return emptyList()

    if (prisonerNumbers.isEmpty()) return emptyList()

    return runCatching {
      nonAssociationsApiWebClient.post()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/non-associations/involving")
            .queryParam("prisonId", prisonCode)
            .build()
        }
        .bodyValue(prisonerNumbers)
        .retrieve()
        .awaitBody<List<NonAssociation>>()
    }.onFailure {
      log.warn("Failed to retrieve non-associations for $prisonCode and involving prisoner numbers $prisonerNumbers", it)
    }.getOrNull()
  }
}
