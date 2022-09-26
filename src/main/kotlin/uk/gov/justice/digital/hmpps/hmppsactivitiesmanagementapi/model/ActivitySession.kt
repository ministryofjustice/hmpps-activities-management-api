package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

@Schema(description =
  "Describes the weekly schedule for an activity. There can be several of these defined for one activity." +
  "An activity session describes when, during the week, an activity will be run and where." +
  "e.g. Tuesday PM and Thursday AM - suitable for Houseblock 2 to attend." +
  "e.g. Monday AM and Thursday PM - suitable for Houseblock 3 to attend."
)
data class ActivitySession(

  @Schema(description = "The internally-generated ID for this activity session", example = "123456")
  val id: Long,

  @Schema(description = "The planned instances associated with this activity session (or schedule).")
  val instances: List<ActivityInstance> = emptyList(),

  @Schema(description = "The list of prisoners who are allocated to this activity, at this time and location")
  val prisoners: List<ActivityPrisoner> = emptyList(),

  @Schema(description = "The description of this activity session", example = "Monday AM Houseblock 3")
  val description: String,

  @Schema(description = "If not null, it indicates the date until which this session is suspended", example = "10/09/2022")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val suspendUntil: LocalDate? = null,

  @Schema(description = "The time that any instances of this session (when scheduled) will start", example = "9:00")
  // TODO: Make this just a time (not a specific date)
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val startTime: LocalDateTime,

  @Schema(description = "The time that any instances of this session (when scheduled) will finish", example = "11:30")
  // TODO: Make this just a time (not a specific date)
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val endTime: LocalDateTime,

  @Schema(description = "The NOMIS internal location id for this session", example = "98877667")
  val internalLocationId: Int? = null,

  @Schema(description = "The NOMIS internal location code for this session", example = "EDU-ROOM-1")
  val internalLocationCode: String? = null,

  @Schema(description = "The NOMIS internal location description for this session", example = "Education - R1")
  val internalLocationDescription: String? = null,

  @Schema(description = "The maximum number of prisoners allowed for a scheduled instance of this session", example = "10")
  val capacity: Int,

  @Schema(description = "The days of the week on which the session takes place", example = "Mon,Tue,Wed")
  val daysOfWeek: String
)
