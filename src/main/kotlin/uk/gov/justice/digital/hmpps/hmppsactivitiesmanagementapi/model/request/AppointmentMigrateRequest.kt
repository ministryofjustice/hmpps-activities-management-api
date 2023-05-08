package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalTime

@Schema(
  description =
  """
  Describes an appointment to be created as part of the NOMIS migration
  """,
)
data class AppointmentMigrateRequest(

  @field:NotEmpty(message = "Prison code must be supplied")
  @field:Size(max = 6, message = "Prison code should not exceed {max} characters")
  @Schema(
    description = "The NOMIS prison code where this appointment takes place",
    example = "PVI",
  )
  val prisonCode: String?,

  @field:NotEmpty(message = "Prisoner number must be supplied")
  @field:Size(max = 10, message = "Prisoner number should not exceed {max} characters")
  @Schema(
    description = "The prisoner to allocate to the appointment",
    example = "A1234BC",
  )
  val prisonerNumber: String,

  @field:NotNull(message = "Booking ID must be supplied")
  @Schema(
    description = "The NOMIS OFFENDER_BOOKINGS.OFFENDER_BOOK_ID value for mapping to a prisoner booking record in NOMIS",
    example = "456",
  )
  val bookingId: Long,

  @field:NotEmpty(message = "Category code must be supplied")
  @field:Size(max = 12, message = "Category code should not exceed {max} characters")
  @Schema(
    description = "The NOMIS reference code for this appointment.",
    example = "CHAP",
  )
  val categoryCode: String,

  @Schema(
    description =
    """
    The NOMIS internal location id within the specified prison.
    """,
    example = "123",
  )
  val internalLocationId: Long?,

  @field:NotNull(message = "Start date must be supplied")
  @Schema(
    description = "The date of the appointment",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @field:NotNull(message = "Start time must be supplied")
  @Schema(
    description = "The starting time of the appointment",
    example = "09:00",
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

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
    """,
    example = "This appointment will help adjusting to life outside of prison",
  )
  val comment: String = "",
)
