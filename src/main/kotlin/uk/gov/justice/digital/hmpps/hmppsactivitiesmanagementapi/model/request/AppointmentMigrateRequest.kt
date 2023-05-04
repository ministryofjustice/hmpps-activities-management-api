package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@Schema(
  description =
  """
  Describes an appointment or series of appointment occurrences to be created, the initial property values and prisoner or
  prisoners to allocate. 
  """,
)
data class AppointmentMigrateRequest(

  @Schema(
    description = "The NOMIS prison code where this appointment takes place",
    example = "PVI",
  )
  val prisonCode: String?,

  @Schema(
    description = "The prisoner to allocate to the appointment",
    example = "A1234BC",
  )
  val prisonerNumber: String,

  @Schema(
    description = "The NOMIS OFFENDER_BOOKINGS.OFFENDER_BOOK_ID value for mapping to a prisoner booking record in NOMIS",
    example = "456",
  )
  val bookingId: Long,

  @Schema(
    description = "The NOMIS reference code for this appointment. Must exist and be active",
    example = "CHAP",
  )
  val categoryCode: String?,

  @Schema(
    description =
    """
    The NOMIS internal location id within the specified prison. This must be supplied if inCell is false.
    The internal location id must exist, must be within the prison specified by the prisonCode property and be active. 
    """,
    example = "123",
  )
  val internalLocationId: Long?,

  @Schema(
    description = "The date of the appointment or first appointment occurrence in the series",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate?,

  @Schema(
    description = "The starting time of the appointment or first appointment occurrence in the series",
    example = "09:00",
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime?,

  @Schema(
    description = "The end time of the appointment",
    example = "10:30",
  )
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(
    description =
    """
    Notes relating to the appointment.
    The default value if no notes are specified at the occurrence or instance levels
    """,
    example = "This appointment will help adjusting to life outside of prison",
  )
  val comment: String = "",
)
