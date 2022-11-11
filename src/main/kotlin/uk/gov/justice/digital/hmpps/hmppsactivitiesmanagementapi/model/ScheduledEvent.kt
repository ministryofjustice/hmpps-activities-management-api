package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "Describes a scheduled event")
data class ScheduledEvent(

  @Schema(description = "The prison code for this scheduled event", example = "MDI")
  val prisonCode: String?,

  @Schema(description = "The event id for this scheduled event", example = "10001")
  val eventId: Long?,

  @Schema(description = "The booking id for this scheduled event", example = "10001")
  val bookingId: Long?,

  @Schema(description = "The location of this scheduled event", example = "INDUCTION CLASSROOM")
  val location: String?,

  @Schema(description = "The location id of this scheduled event", example = "10001")
  val locationId: Long?,

  @Schema(description = "Scheduled event class", example = "INT_MOV")
  val eventClass: String?,

  @Schema(description = "Scheduled event status", example = "SCH")
  val eventStatus: String?,

  @Schema(description = "Scheduled event type", example = "APP")
  val eventType: String?,

  @Schema(description = "Scheduled event type description", example = "Appointment")
  val eventTypeDesc: String?,

  @Schema(description = "Details of this scheduled event", example = "Dont be late")
  val details: String?,

  @Schema(description = "The prisoner number", example = "GF10101")
  val prisonerNumber: String?,

  @Schema(description = "The specific date for this scheduled instance", example = "2022-09-30")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val date: LocalDate?,

  @Schema(description = "The start time for this scheduled instance", example = "9:00")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The end time for this scheduled instance", example = "10:00")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,
)
