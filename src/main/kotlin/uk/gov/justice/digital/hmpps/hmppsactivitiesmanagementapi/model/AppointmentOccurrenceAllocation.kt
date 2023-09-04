package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  Described on the UI as an "Attendee". The allocation of a prisoner to an appointment occurrence.
  """,
)
data class AppointmentOccurrenceAllocation(
  @Schema(
    description =
    """
    The internally generated identifier for this appointment occurrence allocation.
    N.B. this is used as the appointment instance id due to there being a one to one relationship between appointment
    occurrence allocations and appointment instances.
    """,
    example = "123456",
  )
  val id: Long,

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
)
