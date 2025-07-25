package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import java.time.LocalDate
import java.util.UUID

@Schema(
  description =
  """
  Describes the search parameters to use to filter appointments. 
  """,
)
data class AppointmentSearchRequest(
  @Schema(
    description =
    """
    The appointment type (INDIVIDUAL or GROUP) to match with the appointment series. Will restrict the search results to
    appointments that are part of a series with the matching type when this search parameter is supplied.
    """,
    example = "INDIVIDUAL",
  )
  val appointmentType: AppointmentType? = null,

  @field:NotNull(message = "Start date must be supplied")
  @Schema(
    description =
    """
    The start date to match with the appointments. Will restrict the search results to appointments
    that have the matching start date when this search parameter is supplied but no end date is supplied.
    When an end date is also supplied, the search uses a date range and will restrict the search results to
    appointments that have a start date within the date range.
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(
    description = """
    When an end date is supplied alongside the start date, the search uses a date range and will restrict the search results to
    appointments that have a start date within the date range.
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val endDate: LocalDate? = null,

  @Schema(
    description =
    """
    The time slot to match with the appointments. Will restrict the search results to appointments that have a start
    time between the times defined by the prison for that time slot when this search parameter is supplied.
    """,
    example = "[\"AM\",\"PM\",\"ED\"]",
  )
  val timeSlots: List<TimeSlot>? = emptyList(),

  @Schema(
    description =
    """
    The NOMIS reference code to match with the appointments. Will restrict the search results to appointments
    that have the matching category code when this search parameter is supplied.
    """,
    example = "GYMW",
  )
  val categoryCode: String? = null,

  @Schema(
    description =
    """
    The NOMIS internal location id to match with the appointments. Will restrict the search results to
    appointments that have the matching internal location id when this search parameter is supplied.
    """,
    example = "123",
    deprecated = true,
  )
  @Deprecated("Will be removed - use dpsLocationId instead")
  val internalLocationId: Long? = null,

  @Schema(
    description =
    """
    The DPS location UUID to match with the appointments. Will restrict the search results to
    appointments that have the matching location UUID when this search parameter is supplied.
    """,
    example = "b7602cc8-e769-4cbb-8194-62d8e655992a",
  )
  val dpsLocationId: UUID? = null,

  @Schema(
    description =
    """
    The in cell flag value to match with the appointments. Will restrict the search results to appointments
    that have the matching in cell value when this search parameter is supplied.
    """,
    example = "false",
  )
  val inCell: Boolean? = null,

  @Schema(
    description =
    """
    The allocated prisoner or prisoners to match with the appointments. Will restrict the search results to
    appointments that have the at least one of the supplied prisoner numbers attending when this search parameter
    is supplied.
    """,
    example = "[\"A1234BC\"]",
  )
  val prisonerNumbers: List<String>? = null,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth to match with the appointments. Will restrict the
    search results to appointments that were created by this username when this search parameter is supplied.
    """,
    example = "AAA01U",
  )
  val createdBy: String? = null,
)
