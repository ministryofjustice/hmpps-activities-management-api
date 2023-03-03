package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * An Email Address
 * @param email Email
 */
data class Email(

  @Schema(example = "null", description = "Email")
  @JsonProperty("email")
  val email: String? = null,
)
