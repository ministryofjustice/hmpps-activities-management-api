package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Alerts
 * @param alertType Alert Type
 * @param alertCode Alert Code
 * @param active Active
 * @param expired Expired
 */
data class PrisonerAlert(

    @Schema(example = "H", required = true, description = "Alert Type")
    @get:JsonProperty("alertType", required = true) val alertType: String,

    @Schema(example = "HA", required = true, description = "Alert Code")
    @get:JsonProperty("alertCode", required = true) val alertCode: String,

    @Schema(example = "true", required = true, description = "Active")
    @get:JsonProperty("active", required = true) val active: Boolean,

    @Schema(example = "true", required = true, description = "Expired")
    @get:JsonProperty("expired", required = true) val expired: Boolean
)

