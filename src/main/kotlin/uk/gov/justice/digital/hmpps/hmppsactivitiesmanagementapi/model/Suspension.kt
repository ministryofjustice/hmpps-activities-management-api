package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Describes the period of time an activity schedule has been suspended")
data class Suspension(

  @Schema(description = "The date from which the activity schedule was suspended", example = "2022-09-02")
  @JsonFormat(pattern = "yyyy-MM-dd")
  val suspendedFrom: LocalDate,

  @Schema(
    description = "The date until which the activity schedule was suspended. If null, the schedule is suspended indefinitely",
    example = "2022-09-02",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val suspendedUntil: LocalDate? = null,
)
