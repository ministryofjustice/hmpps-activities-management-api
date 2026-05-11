package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model

import com.fasterxml.jackson.annotation.JsonProperty

data class ExternalMovementsResponse(
  @get:JsonProperty("content") val content: List<ExternalMovement>,
)
