package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@Schema(
  description =
  """
  Represents an appointment instance for a specific prisoner to attend at the specified location, date and time.
  The fully denormalised representation of the appointment occurrences and allocations.
  """
)
data class AppointmentInstance (
  @Schema(
    description = "The internally generated identifier for this appointment instance"
  )
  val id: Long,

  @Schema(
    description =
    """
    The lowest level category of the appointment.
    A subcategory for new appointments and a legacy category (active = false) for migrated appointments
    """,
  )
  val category: AppointmentCategory,

  @Schema(
    description = "The NOMIS AGENCY_LOCATIONS.AGY_LOC_ID value for mapping to NOMIS",
    example = "SKI, BNI, WEI"
  )
  val prisonCode: String,

  @Schema(
    description =
    """
    The NOMIS AGENCY_INTERNAL_LOCATIONS.INTERNAL_LOCATION_ID value for mapping to NOMIS.
    Should be null if in cell = true
    """
  )
  val internalLocationId: Int?,

  @Schema(
    description =
    """
    Flag to indicate if the location of the activity is in cell rather than an internal prison location.
    Internal location id should be null if in cell = true
    """
  )
  val inCell: Boolean,

  @Schema(
    description = "The NOMIS OFFENDERS.OFFENDER_ID_DISPLAY value for mapping to a prisoner record in NOMIS",
    example = "A1234BC"
  )
  val prisonerNumber: String,

  @Schema(
    description = "The NOMIS OFFENDER_BOOKINGS.OFFENDER_BOOK_ID value for mapping to a prisoner booking record in NOMIS"
  )
  val bookingId: Int,

  @Schema(
    description = "The date of the appointment instance"
  )
  val appointmentDate: LocalDate,

  @Schema(
    description = "The starting time of the appointment instance"
  )
  val startTime: LocalTime,

  @Schema(
    description = "The end time of the appointment instance"
  )
  val endTime: LocalTime?,

  @Schema(
    description =
    """
    Notes relating to the appointment instance.
    Could support adding a note specific to an individual prisoner's attendance of a specific group appointment
    occurrence. Something that is supported within existing systems
    """,
    example = "This appointment will help prisoner A1234BC adjust to life outside of prison"
  )
  val comment: String,

  @Schema(
    description =
    """
    Simple attendance marking model. Expectation that this will be enhanced to support non attendance reasons in future
    """
  )
  val attended: Boolean,

  @Schema(
    description = "Indicates that the parent appointment occurrence was cancelled"
  )
  val cancelled: Boolean
)
