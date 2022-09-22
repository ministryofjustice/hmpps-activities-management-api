package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

data class ActivityEligibility(

  @Schema(description = "The internal ID for this activity eligibility", example = "123456")
  val id: Long,

  @Schema(description = "The associated eligibility rule for this activity eligibility")
  val eligibility: EligibilityRule
)
