package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  The details of an internal location that has events scheduled to take place there. Supports movement lists.
  Contains additional information about the events taking place at the location.
  The system of record for internal locations is NOMIS and they are managed in that application.
  """,
)
data class InternalLocationEvents(
  @Schema(
    description =
    """
    The id of the internal location. Mapped from AGENCY_INTERNAL_LOCATIONS.INTERNAL_LOCATION_ID in NOMIS.
    """,
    example = "27723",
  )
  val id: Long,

  @Schema(
    description =
    """
    The prison code/agency id of the internal location. Mapped from AGENCY_LOCATIONS.AGY_LOC_ID in NOMIS.
    """,
    example = "SKI",
  )
  val prisonCode: String,

  @Schema(
    description = "The code of the internal location. Mapped from AGENCY_INTERNAL_LOCATIONS.DESCRIPTION",
    example = "EDUC-ED1-ED1",
  )
  val code: String,

  @Schema(
    description = "The description of the internal location. Mapped from AGENCY_INTERNAL_LOCATIONS.USER_DESC",
    example = "Education 1",
  )
  val description: String,

  @Schema(
    description = "Collection of scheduled events due to take place at the internal location",
  )
  var events: Set<ScheduledEvent>,
)
