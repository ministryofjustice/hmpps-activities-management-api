package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern

/**
 * Represents a court hearing for an offender court case.
 * @param id The court hearing identifier.
 * @param dateTime The date and start time of the court hearing in Europe/London (ISO 8601) format without timezone offset e.g. YYYY-MM-DDTHH:MM:SS.
 * @param location
 */
data class CourtHearing(

  @Schema(example = "123456789", description = "The court hearing identifier.")
  @JsonProperty("id")
  val id: Long? = null,

  @get:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
  @Schema(
    example = "2021-07-05T10:35:17",
    description = "The date and start time of the court hearing in Europe/London (ISO 8601) format without timezone offset e.g. YYYY-MM-DDTHH:MM:SS.",
  )
  @JsonProperty("dateTime")
  val dateTime: String? = null,

  @Valid
  @Schema(example = "null", description = "")
  @JsonProperty("location")
  val location: Agency? = null,
)
