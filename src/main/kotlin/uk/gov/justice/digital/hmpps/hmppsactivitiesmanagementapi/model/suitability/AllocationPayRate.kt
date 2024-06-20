package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPayLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation

@Schema(description = "Allocation details with activity pay rate if applicable") // FIXME where is this used
data class AllocationPayRate(
  val payRate: ActivityPayLite? = null,
  val allocation: Allocation,
)
