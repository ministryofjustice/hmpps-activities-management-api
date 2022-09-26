package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

data class ActivitySession(

  @Schema(description = "The internal ID for this activity session", example = "123456")
  val id: Long,

  @Schema(description = "The activity instances associated with this activity session")
  val instances: List<ActivityInstance> = emptyList(),

  @Schema(description = "The prisoners associated with this activity session")
  val prisoners: List<ActivityPrisoner> = emptyList(),

// TODO swagger docs, isn't this on the activity already or is it slightly different?
  @Schema(description = "A detailed description for this activity session", example = "??????")
  val description: String,

  @Schema(description = "The date until the session is suspended", example = "10/09/2022 00:00")
  @JsonFormat(pattern = "dd/MM/yyyy")
  val suspendUntil: LocalDate? = null,

  @Schema(description = "The date and time the session starts", example = "10/09/2022 9:00")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val startTime: LocalDateTime,

  @Schema(description = "The date and time session finishes", example = "10/09/2022 10:00")
  @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
  val endTime: LocalDateTime,

  @Schema(description = "The internal location id for this session", example = "ER1")
  val internalLocationId: Int? = null,

  @Schema(description = "The internal location code for this session", example = "EDU-ROOM-1")
  val internalLocationCode: String? = null,

  @Schema(description = "The internal location description for this session", example = "Education room one")
  val internalLocationDescription: String? = null,

  @Schema(description = "The maximum number of prisoners allowed for the session", example = "10")
  val capacity: Int,

// TODO swagger docs, should example be 1010000
  @Schema(description = "The days of the week on which the session takes place", example = "Monday, Wednesday")
  val daysOfWeek: String
)
