package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentType
import java.time.LocalDate

@Schema(
  description =
  """
  Describes the search parameters to use to filter appointment occurrences. 
  """,
)
data class AppointmentOccurrenceSearchRequest(
  @Schema(
    description =
    """
    The appointment type (INDIVIDUAL or GROUP) match with the parent appointments. Will restrict the search results to
    appointment occurrences that have a parent appointment with the matching type when this search parameter is supplied.
    """,
    example = "INDIVIDUAL",
  )
  val appointmentType: AppointmentType? = null,

  @field:NotNull(message = "Start date must be supplied")
  @Schema(
    description =
    """
    The start date to match with the appointment occurrences. Will restrict the search results to appointment
    occurrences that have the matching start date when this search parameter is supplied but no end date is supplied.
    When an end date is also supplied, the search uses a date range and will restrict the search results to appointment
    occurrences that have a start date within the date range.
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate? = null,

  @Schema(
    description =
    """
    The end date of the date range to match with the appointment occurrences. Start date must be supplied if an end date
    is specified. Will restrict the search results to appointment occurrences that have a start date within the date range.
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val endDate: LocalDate? = null,

  @Schema(
    description =
    """
    The time slot to match with the appointment occurrences. Will restrict the search results to appointment occurrences
    that have a start time between the times defined by the prison for that time slot when this search parameter is
    supplied.
    """,
    example = "PM",
  )
  val timeSlot: TimeSlot? = null,

  @Schema(
    description =
    """
    The NOMIS reference code to match with the parent appointments. Will restrict the search results to appointment
    occurrences that have a parent appointment with the matching category code when this search parameter is supplied.
    """,
    example = "GYMW",
  )
  val categoryCode: String? = null,

  @Schema(
    description =
    """
    The NOMIS internal location id to match with the appointment occurrences. Will restrict the search results to
    appointment occurrences that have the matching internal location id when this search parameter is supplied.
    """,
    example = "123",
  )
  val internalLocationId: Long? = null,

  @Schema(
    description =
    """
    The in cell flag value to match with the appointment occurrences. Will restrict the search results to appointment
    occurrences that have the matching in cell value when this search parameter is supplied.
    """,
    example = "false",
  )
  val inCell: Boolean? = null,

  @Schema(
    description =
    """
    The allocated prisoner or prisoners to match with the appointment occurrences. Will restrict the search results to
    appointment occurrences that have the at least one of the supplied prisoner numbers allocated to them when this
    search parameter is supplied.
    """,
    example = "[\"A1234BC\"]",
  )
  val prisonerNumbers: List<String>? = null,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth to match with the parent appointments. Will restrict the
    search results to appointment occurrences that have a parent appointment created by this username when this search
    parameter is supplied.
    """,
    example = "AAA01U",
  )
  val createdBy: String? = null,
)
