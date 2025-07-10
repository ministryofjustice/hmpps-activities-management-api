package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

data class ActivitySuitabilityCriteria(
  @Schema(description = "The most recent risk assessment level for this activity", example = "high")
  val riskLevel: String,
  @Schema(description = "Whether the activity is a paid activity", example = "true")
  val isPaid: Boolean,
  @Schema(description = "Describes the pay rates and bands which apply to an activity")
  val payRates: List<ActivityPay>,
  @Schema(description = "Describes the minimum education levels which apply to an activity")
  val minimumEducationLevel: List<ActivityMinimumEducationLevel>,
)
