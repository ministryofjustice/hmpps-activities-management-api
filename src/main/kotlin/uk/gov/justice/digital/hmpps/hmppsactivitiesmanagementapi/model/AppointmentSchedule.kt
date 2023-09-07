package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Schema(
  description =
  """
  Describes the schedule of the appointment i.e. how the appointments in the series will repeat. The frequency of
  those appointments and how many appointments there will be in total in the series.
  """,
)
data class AppointmentSchedule(
  @field:NotNull(message = "Frequency must be supplied")
  @Schema(
    description =
    """
    The frequency of the appointments in the repeating appointment series. When they will repeat and how often
    """,
    example = "WEEKLY",
  )
  val frequency: AppointmentFrequency?,
  @field:NotNull(message = "Number of appointments must be supplied")
  @field:Min(value = 1, message = "Number of appointments must be {value} or greater")
  @Schema(
    description =
    """
    The total number of appointments in the appointment series
    """,
    example = "6",
  )
  val count: Int?,
)
