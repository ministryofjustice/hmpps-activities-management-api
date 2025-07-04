package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

@Schema(
  description =
  """
  Summarises an appointment location for display purposes. Contains only properties needed to make additional API calls
  and to display. NOMIS is the current system of record for appointment locations and they are managed there.
  """,
)
data class AppointmentLocationSummary(
  @Schema(
    description = "The NOMIS AGENCY_INTERNAL_LOCATIONS.INTERNAL_LOCATION_ID value for mapping to NOMIS.",
    example = "27",
  )
  val id: Long,

  @Schema(description = "The optional DPS location UUID", example = "b7602cc8-e769-4cbb-8194-62d8e655992a")
  val dpsLocationId: UUID? = null,

  @Schema(
    description =
    """
    The NOMIS AGENCY_LOCATIONS.AGY_LOC_ID value for mapping to NOMIS.
    """,
    example = "SKI",
  )
  val prisonCode: String,

  @Schema(
    description = "The description of the appointment location. Mapped from AGENCY_INTERNAL_LOCATIONS.USER_DESC",
    example = "Chapel",
  )
  val description: String,
)
