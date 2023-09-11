package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(
  description =
  """
  Described on the UI as an "Appointment set" or "set of back-to-back appointments".
  Contains the initial property values common to all appointment series and appointments in the set.
  The properties at this level cannot be changed via the API.
  """,
)
data class AppointmentSet(
  @Schema(
    description = "The internally generated identifier for this appointment set",
    example = "12345",
  )
  val id: Long,

  @Schema(
    description = "The NOMIS AGENCY_LOCATIONS.AGY_LOC_ID value for mapping to NOMIS",
    example = "SKI",
  )
  val prisonCode: String,

  @Schema(
    description = "The NOMIS REFERENCE_CODES.CODE (DOMAIN = 'INT_SCH_RSN') value for mapping to NOMIS",
    example = "CHAP",
  )
  val categoryCode: String,

  @Schema(
    description =
    """
    Free text name further describing the appointment set. Used as part of the appointment name with the
    format "Custom name (Category description) if specified.
    """,
    example = "Meeting with the governor",
  )
  val customName: String?,

  @Schema(
    description =
    """
    The NOMIS AGENCY_INTERNAL_LOCATIONS.INTERNAL_LOCATION_ID value for mapping to NOMIS.
    Will be null if in cell = true
    """,
    example = "123",
  )
  val internalLocationId: Long?,

  @Schema(
    description =
    """
    Flag to indicate if the location of the appointment set is in cell rather than an internal prison location.
    Internal location id should be null if in cell = true
    """,
    example = "false",
  )
  val inCell: Boolean,

  @Schema(
    description = "The date of the first appointment in each appointment series in the set",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(
    description = "The appointments in the set",
  )
  val appointments: List<Appointment>,

  @Schema(
    description = "The date and time this appointment set was created. Will not change",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdTime: LocalDateTime,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that created this appointment set.
    Usually a NOMIS username
    """,
    example = "AAA01U",
  )
  val createdBy: String,
)
