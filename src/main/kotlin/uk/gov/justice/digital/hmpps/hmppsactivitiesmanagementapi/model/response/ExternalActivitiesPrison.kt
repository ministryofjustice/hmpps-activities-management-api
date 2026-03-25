package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Prisons enabled for External Activities")
data class ExternalActivitiesPrison(
  @Schema(description = "The prison code", example = "AGI")
  val prisonCode: String,

  @Schema(description = "The name of the prison", example = "Askham Grange")
  val prisonName: String,
)
