package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.externalmovementsapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

data class ExternalMovementsRequest(
  @get:JsonProperty("personIdentifiers") val prisonerNumbers: List<String>,
  @get:JsonProperty("start") val start: LocalDateTime,
  @get:JsonProperty("end") val end: LocalDateTime,
)
