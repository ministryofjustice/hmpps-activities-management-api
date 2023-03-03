package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  The allocation of a prisoner to an appointment occurrence. Standard single appointments will have one prisoner
  allocated to its single appointment occurrence. More than one prisoner allocation record signifies the associated
  appointment is a group appointment. Group appointments support additional checks such as non-associations.
  """,
)
data class AppointmentOccurrenceAllocation(
  @Schema(
    description = "The internally generated identifier for this appointment allocation",
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
