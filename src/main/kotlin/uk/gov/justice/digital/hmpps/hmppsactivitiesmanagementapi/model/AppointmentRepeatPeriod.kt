package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

enum class AppointmentRepeatPeriod {
  @Schema(
    description = "Repeats every day Monday to Friday",
  )
  WEEKDAY,

  @Schema(
    description = "Repeats every day including weekends",
  )
  DAILY,

  @Schema(
    description = "Repeats weekly on the day of the week specified by the first appointment occurrence's date",
  )
  WEEKLY,

  @Schema(
    description = "Repeats once every two weeks on the day of the week specified by the first appointment occurrence's date",
  )
  FORTNIGHTLY,

  @Schema(
    description = "Repeats once a month on the day of the month specified by the first appointment occurrence's date",
  )
  MONTHLY,
}
