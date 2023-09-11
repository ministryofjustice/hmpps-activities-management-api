package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

enum class AppointmentFrequency {
  @Schema(
    description = "Repeats every day Monday to Friday",
  )
  WEEKDAY,

  @Schema(
    description = "Repeats every day including weekends",
  )
  DAILY,

  @Schema(
    description = "Repeats weekly on the day of the week specified by the date of the first appointment",
  )
  WEEKLY,

  @Schema(
    description = "Repeats once every two weeks on the day of the week specified by the date of the first appointment",
  )
  FORTNIGHTLY,

  @Schema(
    description = "Repeats once a month on the day of the month specified by the date of the first appointment",
  )
  MONTHLY,
}
