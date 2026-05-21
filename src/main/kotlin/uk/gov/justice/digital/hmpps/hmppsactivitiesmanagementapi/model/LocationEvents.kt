package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(
  description =
  """
  The details of locations that have scheduled events for movement lists. Used for movement lists.
  For internal locations, all fields are populated. 
  For external movements, id and dpsLocationId will be null and the location will be represented as OUTSIDE/Outside
  """,
)
data class LocationEvents(
  @Schema(
    description = "The id of the internal location. Null for external movements.",
    example = "27723",
  )
  val id: Long?,

  @Schema(description = "The DPS location UUID. Null for external movements.", example = "b7602cc8-e769-4cbb-8194-62d8e655992a")
  val dpsLocationId: UUID?,

  @Schema(
    description = "The prison code/agency id.",
    example = "SKI",
  )
  val prisonCode: String,

  @Schema(
    description = "The code of the location. For external movements this will be 'OUTSIDE'.",
    example = "EDUC-ED1-ED1",
  )
  val code: String,

  @Schema(
    description = "The description of the location. For external movements this will be 'Outside'.",
    example = "Education 1",
  )
  val description: String,

  @Schema(
    description = "Collection of scheduled events due to take place at the location",
  )
  var events: Set<ScheduledEvent>,
)
