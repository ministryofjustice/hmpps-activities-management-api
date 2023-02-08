package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Incentive level
 * @param level
 * @param dateTime Date time of the incentive
 * @param nextReviewDate Schedule new review date
 */
data class CurrentIncentive(
  @Schema(example = "null", required = true, description = "")
  @get:JsonProperty("level", required = true) val level: IncentiveLevel,

  @Schema(example = "2021-07-05T10:35:17", required = true, description = "Date time of the incentive")
  @get:JsonProperty("dateTime", required = true) val dateTime: kotlin.String,

  @Schema(example = "Thu Nov 10 00:00:00 GMT 2022", required = true, description = "Schedule new review date")
  @get:JsonProperty("nextReviewDate", required = true) val nextReviewDate: java.time.LocalDate
)
