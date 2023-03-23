package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "Describes a prisons scheduled events")
data class PrisonerScheduledEvents(

  @Schema(description = "The prison code for these scheduled events", example = "MDI")
  val prisonCode: String?,

  @Schema(description = "The set of prisoner numbers for theses scheduled events ", example = "['GF10101', 'GR123YI']")
  val prisonerNumbers: Set<String>?,

  @Schema(description = "The start date for this collection of scheduled events", example = "2022-11-01")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val startDate: LocalDate?,

  @Schema(description = "The end date (inclusive) for this collection of scheduled events", example = "2022-11-28")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val endDate: LocalDate?,

  @Schema(description = "A list of scheduled appointments for this prisoner in this date range")
  val appointments: List<ScheduledEvent>? = null,

  @Schema(description = "A list of (active) scheduled court hearings for this prisoner in this date range")
  val courtHearings: List<ScheduledEvent>? = null,

  @Schema(description = "A list of scheduled visits for this prisoner in this date range")
  val visits: List<ScheduledEvent>? = null,

  @Schema(description = "A list of scheduled activities for this prisoner in this date range")
  var activities: List<ScheduledEvent>? = null,

  @Schema(description = "A list of external transfers for this prisoner in this date range")
  val externalTransfers: List<ScheduledEvent>? = null,
)
