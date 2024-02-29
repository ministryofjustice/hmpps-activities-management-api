package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(
  description =
  """
  Described on the UI as an "Attendee". A prisoner attending a specific appointment in an appointment series or set.
  Contains the limited summary information needed to display the prisoner information and whether they attended or not.
  """,
)
data class AppointmentAttendeeSummary(
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
    description = "Summary of the prisoner attending the appointment",
  )
  val prisoner: PrisonerSummary,

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
    The summary of the user that last recorded attendance. Note that attendance records can be updated and this is the
    most recent user that marked attendance. A null value means that the prisoner's attendance has not been recorded yet.
    """,
    example = "AAA01U",
  )
  val attendanceRecordedBy: String?,
)
