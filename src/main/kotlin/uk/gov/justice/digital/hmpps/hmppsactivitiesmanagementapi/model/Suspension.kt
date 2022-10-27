package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Describes the period of time an activity schedule has been suspended")
data class Suspension(

  @Schema(description = "The date from which the activity schedule was suspended", example = "02/09/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val suspendedFrom: LocalDate,

  @Schema(
    description = "The date until which the activity schedule was suspended. If null, the schedule is suspended indefinately",
    example = "02/09/2022"
  )
  @JsonFormat(pattern = "dd/MM/yyyy")
  val suspendedUntil: LocalDate? = null,
)
