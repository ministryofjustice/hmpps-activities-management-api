package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
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
    The start date to match with the appointment occurrences. Will restrict the search results to appointment
    occurrences that have the matching start date when this search parameter is supplied.
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate? = null,

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
)
