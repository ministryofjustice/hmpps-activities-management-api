package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(
  description = "Describes a set of appointments created as part of a single bulk operation",
)
data class BulkAppointmentDetails(

  @Schema(
    description = "The internally generated identifier for this set of appointments",
    example = "12345",
  )
  val id: Long,

  @Schema(
    description =
    """
    The NOMIS AGENCY_LOCATIONS.AGY_LOC_ID value for mapping to NOMIS.
    Note, this property does not exist on the appointment occurrences and is therefore consistent across all occurrences
    """,
    example = "SKI",
  )
  val prisonCode: String,

  @Schema(
    description =
    """
    The appointment name
    """,
  )
  val appointmentName: String,

  @Schema(
    description =
    """
    The summary of the category used to create the set of appointments in bulk
    """,
  )
  val category: AppointmentCategorySummary,

  @Schema(
    description =
    """
    Free text description used to create the set of appointments in bulk. This is used to add more context to the appointment category.
    """,
    example = "Meeting with the governor",
  )
  val appointmentDescription: String?,

  @Schema(
    description =
    """
    The summary of the internal location used to create the set of appointments in bulk. Will be null if in cell = true
    """,
  )
  val internalLocation: AppointmentLocationSummary?,

  @Schema(
    description =
    """
    Flag to indicate if the location used to create the set of appointments in bulk was in cell rather than an internal prison location.
    Internal location will be null if in cell = true
    """,
    example = "false",
  )
  val inCell: Boolean,

  @Schema(
    description = "The date used to create the set of appointments in bulk",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(
    description = "The details of the set of appointment occurrences created in bulk",
  )
  val occurrences: List<AppointmentOccurrenceDetails>,

  @Schema(
    description = "The date and time this set of appointments was created in bulk. Will not change",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val created: LocalDateTime,

  @Schema(
    description =
    """
    The summary of the user that created this set of appointments in bulk
    """,
  )
  val createdBy: UserSummary,
)
