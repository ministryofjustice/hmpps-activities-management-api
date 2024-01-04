package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation

@Schema(description = "Ended allocation details with optional deallocation case note text")
data class DeallocationCaseNote(
  val allocation: Allocation,
  val caseNoteText: String? = null,
)
