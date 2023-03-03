package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes an eligibility rule as applied to an activity")
data class ActivityEligibility(

  @Schema(description = "The internal ID of the activity that these rules apply to", example = "123456")
  val id: Long,

  @Schema(description = "The eligiblity rule which applies")
  val eligibility: EligibilityRule,
)
