package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(
  description =
  """
  Describes the recurrence of an appointment. The days of the week an occurrence of the appointment will be scheduled
  and the end date of the series.
  """
)
data class AppointmentSchedule(
  @Schema(
    description = "The internally generated identifier for this appointment schedule",
    example = "12345"
  )
  val id: Long,

  @Schema(
    description =
    """
    The date the series of appointment occurrences should end. The UI will provide options to specify an end date or
    a number of occurrences. The later case should be used to calculate the end date internally
    """
  )
  val endDate: LocalDate,

  @Schema(
    description = "Indicates the appointment reoccurs every Monday",
    example = "false"
  )
  val mondayFlag: Boolean,

  @Schema(
    description = "Indicates the appointment reoccurs every Tuesday",
    example = "true"
  )
  val tuesdayFlag: Boolean,

  @Schema(
    description = "Indicates the appointment reoccurs every Wednesday",
    example = "false"
  )
  val wednesdayFlag: Boolean,

  @Schema(
    description = "Indicates the appointment reoccurs every Thursday",
    example = "false"
  )
  val thursdayFlag: Boolean,

  @Schema(
    description = "Indicates the appointment reoccurs every Friday",
    example = "false"
  )
  val fridayFlag: Boolean,

  @Schema(
    description = "Indicates the appointment reoccurs every Saturday",
    example = "false"
  )
  val saturdayFlag: Boolean,

  @Schema(
    description = "Indicates the appointment reoccurs every Sunday",
    example = "false"
  )
  val sundayFlag: Boolean
)
