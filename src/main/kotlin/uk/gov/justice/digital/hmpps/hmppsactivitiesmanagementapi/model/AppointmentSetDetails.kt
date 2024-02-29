package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(
  description =
  """
  Described on the UI as an "Appointment set" or "set of back-to-back appointments".
  Contains the full details of the initial property values common to all appointments in the set for display purposes.
  The properties at this level cannot be changed via the API.
  """,
)
data class AppointmentSetDetails(
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
    description =
    """
    The appointment set's name combining the optional custom name with the category description. If custom name has been
    specified, the name format will be "Custom name (Category description)" 
    """,
  )
  val appointmentName: String,

  @Schema(
    description =
    """
    The summary of the category used to create the set of appointment series
    """,
  )
  val category: AppointmentCategorySummary,

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
    The summary of the internal location of the appointment series in the set. Will be null if in cell = true
    """,
  )
  val internalLocation: AppointmentLocationSummary?,

  @Schema(
    description =
    """
    Flag to indicate if the location of the appointment series in the set is in cell rather than an internal prison location.
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
    description = "The details of all the appointments in the the set",
  )
  val appointments: List<AppointmentDetails>,

  @Schema(
    description = "The date and time this appointment set was created. Will not change",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdTime: LocalDateTime,

  @Schema(
    description =
    """
    The username of the user that created this appointment set
    """,
  )
  val createdBy: String,

  @Schema(
    description =
    """
    The date and time one or more appointments in this set was last changed.
    Will be null if no appointments in the set have been altered since they were created
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updatedTime: LocalDateTime?,

  @Schema(
    description =
    """
    The username of the user that last edited one or more appointments in this set.
    Will be null if no appointments in the set have been altered since they were created
    """,
  )
  val updatedBy: String?,
)
