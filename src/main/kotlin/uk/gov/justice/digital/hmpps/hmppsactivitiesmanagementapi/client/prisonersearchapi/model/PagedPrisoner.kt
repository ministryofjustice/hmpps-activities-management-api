package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model

import com.fasterxml.jackson.annotation.JsonProperty

data class PagedPrisoner(
  @get:JsonProperty("content", required = true) val content: List<Prisoner>,
)
