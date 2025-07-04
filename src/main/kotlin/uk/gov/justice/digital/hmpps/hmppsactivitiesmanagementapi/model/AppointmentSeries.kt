package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

@Schema(
  description =
  """
  Described on the UI as an "Appointment series" and only shown for repeat appointments.
  The top level of the standard appointment hierarchy containing the initial property values common to all appointments
  in the series.
  Contains the collection of all the child appointments in the series plus the schedule definition if the appointment series repeats.
  The properties at this level cannot be changed via the API however the child appointment property values can be changed
  independently to support rescheduling, cancelling and altered attendee lists per appointment.
  N.B. there is no collection of attending prisoners at this top level as all attendees are per appointment. This is to
  support attendee modification for each scheduled appointment and to prevent altering the past by editing attendees
  in an appointment series where some appointments have past.
  """,
)
data class AppointmentSeries(
  @Schema(
    description = "The internally generated identifier for this appointment series",
    example = "12345",
  )
  val id: Long,

  @Schema(
    description = "The appointment type (INDIVIDUAL or GROUP)",
    example = "INDIVIDUAL",
  )
  val appointmentType: AppointmentType,

  @Schema(
    description = "The NOMIS AGENCY_LOCATIONS.AGY_LOC_ID value for mapping to NOMIS",
    example = "SKI",
  )
  val prisonCode: String,

  @Schema(
    description = "The NOMIS REFERENCE_CODES.CODE (DOMAIN = 'INT_SCH_RSN') value for mapping to NOMIS",
    example = "CHAP",
  )
  val categoryCode: String,

  @Schema(description = "The tier for this appointment, as defined by the Future Prison Regime team")
  val tier: EventTier?,

  @Schema(description = "The organiser of this appointment")
  val organiser: EventOrganiser?,

  @Schema(
    description =
    """
    Free text name further describing the appointment series. Used as part of the appointment name with the
    format "Custom name (Category description) if specified.
    """,
    example = "Meeting with the governor",
  )
  val customName: String?,

  @Schema(
    description =
    """
    The NOMIS AGENCY_INTERNAL_LOCATIONS.INTERNAL_LOCATION_ID value for mapping to NOMIS.
    Will be null if in cell = true
    """,
    example = "123",
  )
  val internalLocationId: Long?,

  @Schema(
    description =
    """
    Flag to indicate if the location of the appointment series is in cell rather than an internal prison location.
    Internal location id should be null if in cell = true
    """,
    example = "false",
  )
  val inCell: Boolean,

  @Schema(
    description = "The date of the first appointment in the series",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val startDate: LocalDate,

  @Schema(
    description = "The starting time of the appointment or appointments in the series",
    example = "09:00",
  )
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(
    description = "The end time of the appointment or appointments in the series",
    example = "10:30",
  )
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

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
    Extra information for the prisoner or prisoners attending the appointment or appointments in the series.
    Shown only on the appointments details page and on printed movement slips. Wing staff will be notified there is
    extra information via the unlock list.
    """,
    example = "This appointment will help adjusting to life outside of prison",
  )
  val extraInformation: String?,

  @Schema(
    description = "The date and time this appointment series was created. Will not change",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val createdTime: LocalDateTime,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that created the appointment series.
    Usually a NOMIS username
    """,
    example = "AAA01U",
  )
  val createdBy: String,

  @Schema(
    description =
    """
    The date and time one or more appointments in this series was last changed.
    Will be null if no appointments in the series have been altered since they were created
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val updatedTime: LocalDateTime?,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that last edited one or more appointments in this series.
    Will be null if no appointments in the series have been altered since they were created
    """,
    example = "AAA01U",
  )
  val updatedBy: String?,

  @Schema(
    description =
    """
    The individual appointment or appointments in this series. Non recurring appointment series will have a single
    appointment containing the same property values as the parent appointment series. The same start date, time
    and end time. Recurring appointment series will have one or more appointments. The first in the series will also
    contain the same property values as the parent appointment series and subsequent appointments will have start dates
    following on from the original start date incremented as specified by the series' schedule. Each appointment
    can be edited independently of the parent. All properties of an appointment are separate to those of the parent
    appointment series. The full series of appointments specified by the schedule will have been created in advance.
    """,
  )
  val appointments: List<Appointment> = emptyList(),

  @Schema(description = "The optional DPS location UUID for this appointment series", example = "b7602cc8-e769-4cbb-8194-62d8e655992a")
  val dpsLocationId: UUID? = null,
)
