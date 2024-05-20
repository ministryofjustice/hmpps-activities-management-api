package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

data class PrisonerSummary(
  @Schema(
    description = "The NOMIS OFFENDERS.OFFENDER_ID_DISPLAY value for mapping to a prisoner record in NOMIS",
    example = "A1234BC",
  )
  val prisonerNumber: String,

  @Schema(
    description = "The NOMIS OFFENDER_BOOKINGS.OFFENDER_BOOK_ID value for mapping to a prisoner booking record in NOMIS",
    example = "456",
  )
  val bookingId: Long,

  @Schema(
    description = "The prisoner's first name",
    example = "Albert",
  )
  val firstName: String,

  @Schema(
    description = "The prisoner's first name",
    example = "Abbot",
  )
  val lastName: String,

  @Schema(
    description = "The prisoner's status at their current prison",
    example = "ACTIVE IN",
  )
  val status: String,

  @Schema(
    description =
    """
    The NOMIS AGENCY_LOCATIONS.AGY_LOC_ID value for mapping to NOMIS.
    """,
    example = "SKI",
  )
  val prisonCode: String,

  @Schema(
    description =
    """
    The prisoner's residential cell location when inside the prison.
    """,
    example = "A-1-002",
  )
  val cellLocation: String,

  @Schema(
    description =
    """
    The prisoner's category.
    """,
    example = "P",
  )
  val category: String,
)
