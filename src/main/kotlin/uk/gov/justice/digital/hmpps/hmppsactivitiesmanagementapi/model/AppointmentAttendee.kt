package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
  description =
  """
  Described on the UI as an "Attendee". A prisoner attending a specific appointment in an appointment series or set.
  """,
)
data class AppointmentAttendee(
  @Schema(
    description =
    """
    The internally generated identifier for this prisoner attending a specific appointment in an appointment series or set.
    N.B. this is used as the appointment instance id due to there being a one to one relationship between an appointment
    attendee and appointment instances.
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

  @Schema(
    description =
    """
    The date and time this attendee was added appointment.
    Will be null if this attendee was part of the appointment when the appointment was created
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val addedTime: LocalDateTime?,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that added this attendee to the appointment.
    Will be null if this attendee was part of the appointment when the appointment was created
    """,
    example = "AAA01U",
  )
  val addedBy: String?,

  @Schema(
    description =
    """
    Specifies whether the prisoner attended the specific appointment in an appointment series or set.
    A null value means that the prisoner's attendance has not been recorded yet. 
    """,
  )
  var attended: Boolean?,

  @Schema(
    description =
    """
    The latest date and time attendance was recorded. Note that attendance records can be updated and this is the most
    recent date and time it was recorded. A null value means that the prisoner's attendance has not been recorded yet. 
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val attendanceRecordedTime: LocalDateTime?,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that last recorded attendance. Note that attendance records
    can be updated and this is the most recent user that marked attendance. A null value means that the prisoner's
    attendance has not been recorded yet. 
    """,
    example = "AAA01U",
  )
  val attendanceRecordedBy: String?,

  @Schema(
    description =
    """
    The date and time this attendee was removed from the appointment.
    Will be null if this attendee has not been removed from the appointment
    """,
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val removedTime: LocalDateTime?,

  @Schema(
    description =
    """
    The id of the reason why this attendee was removed from the appointment.
    Will be null if this attendee has not been removed from the appointment
    """,
    example = "12345",
  )
  val removalReasonId: Long?,

  @Schema(
    description =
    """
    The username of the user authenticated via HMPPS auth that removed this attendee from the appointment.
    Will be null if this attendee has not been removed from the appointment
    """,
    example = "AAA01U",
  )
  val removedBy: String?,
)
