package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
  description =
  """
  Described on the UI as an "Appointment series" and only shown for repeat appointments.
  The top level of the standard appointment hierarchy containing summary information of a limited set of the initial
  property values common to all appointments in the series as well as the count of appointments in the series.
  The properties at this level cannot be changed via the API however the child appointment property values can be changed
  independently to support rescheduling, cancelling and altered attendee lists per appointment.
  N.B. there is no collection of attending prisoners at this top level as all attendees are per appointment. This is to
  support attendee modification for each scheduled appointment and to prevent altering the past by editing attendees
  in an appointment series where some appointments have past.
  """,
)
data class AppointmentSeriesSummary(
  @Schema(
    description = "The internally generated identifier for this appointment series",
    example = "12345",
  )
  val id: Long,

  @Schema(
    description =
    """
    Describes the schedule of the appointment series i.e. how the appointments in the series repeat. The frequency of
    those appointments and how many appointments there are in total in the series.
    If null, the appointment series has only one appointment. Note that the presence of this property does not mean
    there is more than one appointment as a number of appointments value of one is valid.
    """,
  )
  val schedule: AppointmentSeriesSchedule?,

  @Schema(
    description =
    """
    The total count of appointments in the series that have not been deleted. Counts both appointments in the past and
    those scheduled.
    """,
  )
  val appointmentCount: Int,

  @Schema(
    description =
    """
    The count of the remaining scheduled appointments in the series that have not been cancelled or deleted.
    """,
  )
  val scheduledAppointmentCount: Int,
)
