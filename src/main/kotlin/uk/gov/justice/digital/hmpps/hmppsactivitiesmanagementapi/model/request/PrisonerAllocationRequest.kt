package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

data class PrisonerAllocationRequest(
  @Schema(description = "The internally-generated ID for this activity schedule", example = "123456")
  val scheduleId: Long,

  @Schema(description = "The prisoner number (Nomis ID)", example = "A1234AA")
  val prisonerNumber: String,
)
