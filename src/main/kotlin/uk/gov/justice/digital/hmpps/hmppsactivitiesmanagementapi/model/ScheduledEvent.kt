package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "Describes a scheduled event")
data class ScheduledEvent(

  @Schema(description = "The prison code for this scheduled event", example = "MDI")
  val prisonCode: String?,

  @Schema(description = "The source of this event - valid values are NOMIS or SAA (scheduling activities and appointments)", example = "NOMIS")
  val eventSource: String?,

  @Schema(description = "The event type (APPOINTMENT, ACTIVITY, COURT_HEARING, EXTERNAL_TRANSFER, ADJUDICATION_HEARING, VISIT)", example = "APPOINTMENT")
  val eventType: String?,

  @Schema(description = "For activities from SAA the ID for the activity scheduled instance, or null when from NOMIS", example = "9999")
  val scheduledInstanceId: Long?,

  @Schema(description = "For appointments from SAA the ID for the appointment, or null when from NOMIS", example = "9999")
  val appointmentId: Long?,

  @Schema(description = "For appointments from SAA the ID for the appointment occurrence, or null when from NOMIS", example = "9999")
  val appointmentOccurrenceId: Long?,

  @Schema(description = "For appointments from SAA the ID for the appointment instance, or null when from NOMIS", example = "9999")
  val appointmentInstanceId: Long?,

  @Schema(description = "For appointments from SAA the optional appointment description", example = "Meeting with the governor")
  val appointmentDescription: String?,

  @Schema(description = "For adjudication hearings from NOMIS the ID for the OIC hearing, or null for other types", example = "9999")
  val oicHearingId: Long?,

  @Schema(description = "The event ID for events from NOMIS, otherwise null if from SAA", example = "10001")
  val eventId: Long?,

  @Schema(description = "The booking id of the prisoner this event relates to.", example = "10001")
  val bookingId: Long?,

  @Schema(description = "The NOMIS internal location id where this event takes place", example = "10001")
  val internalLocationId: Long?,

  @Schema(description = "The NOMIS location code for this event", example = "MDI-HB1-EDUCATION-RM1")
  val internalLocationCode: String?,

  @Schema(description = "The NOMIS location description for this event", example = "Education Room One")
  val internalLocationDescription: String?,

  @Schema(description = "Event category code (e.g appointment category code, activity category code)", example = "GOVE")
  val categoryCode: String?,

  @Schema(description = "Event category description.", example = "Governor")
  val categoryDescription: String?,

  @Schema(description = "The event summary to show on unlock lists, schedules and calendars", example = "Court hearing")
  val summary: String?,

  @Schema(description = "Any comments supplied that relate to this event", example = "Reception for 8am please.")
  val comments: String?,

  @Schema(description = "Set to true if this event will take place in the prisoner's cell", example = "false")
  val inCell: Boolean = false,

  @Schema(description = "Flag to indicate if the location of the activity is on wing", example = "false")
  var onWing: Boolean = false,

  @Schema(description = "Set to true if this event takes place outside the prison", example = "false")
  val outsidePrison: Boolean = false,

  @Schema(description = "Set to true if this event has been cancelled", example = "false")
  val cancelled: Boolean = false,

  @Schema(description = "Set to true if this prisoner is suspended for this event (only applies to activities)", example = "false")
  val suspended: Boolean = false,

  @Schema(description = "The prisoner number", example = "GF10101")
  val prisonerNumber: String?,

  @Schema(description = "The specific date for this event", example = "2022-09-30")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val date: LocalDate?,

  @Schema(description = "The start time for this scheduled instance", example = "09:00")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The end time for this scheduled instance", example = "10:00")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(description = "The event priority - configurable by prison, or via defaults.")
  val priority: Int,
)
