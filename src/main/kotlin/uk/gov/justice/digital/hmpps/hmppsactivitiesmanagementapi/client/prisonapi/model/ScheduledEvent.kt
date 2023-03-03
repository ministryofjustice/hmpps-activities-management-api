package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern

/**
 * Scheduled Event
 * @param bookingId Offender booking id
 * @param eventClass Class of event
 * @param eventStatus Status of event
 * @param eventType Type of scheduled event (as a code)
 * @param eventTypeDesc Description of scheduled event type
 * @param eventSubType Sub type (or reason) of scheduled event (as a code)
 * @param eventSubTypeDesc Description of scheduled event sub type
 * @param eventDate Date on which event occurs
 * @param eventSource Code identifying underlying source of event data
 * @param eventId Activity id if any. Used to attend or pay an activity.
 * @param startTime Date and time at which event starts
 * @param endTime Date and time at which event ends
 * @param eventLocation Location at which event takes place (could be an internal location, agency or external address).
 * @param eventLocationId Id of an internal event location
 * @param agencyId The agency ID for the booked internal location
 * @param eventSourceCode Source-specific code for the type or nature of the event
 * @param eventSourceDesc Source-specific description for type or nature of the event
 * @param eventOutcome Activity attendance, possible values are the codes in the 'PS_PA_OC' reference domain.
 * @param performance Activity performance, possible values are the codes in the 'PERFORMANCE' reference domain.
 * @param outcomeComment Activity no-pay reason.
 * @param paid Activity paid flag.
 * @param payRate Amount paid per activity session in pounds
 * @param locationCode The code for the activity location
 * @param createUserId Staff member who created the appointment
 */
data class ScheduledEvent(

  @Schema(example = "null", description = "Offender booking id")
  @JsonProperty("bookingId")
  val bookingId: Long,

  @Schema(example = "null", description = "Class of event")
  @JsonProperty("eventClass")
  val eventClass: String,

  @Schema(example = "null", description = "Status of event")
  @JsonProperty("eventStatus")
  val eventStatus: String,

  @Schema(example = "null", description = "Type of scheduled event (as a code)")
  @JsonProperty("eventType")
  val eventType: String,

  @Schema(example = "null", description = "Description of scheduled event type")
  @JsonProperty("eventTypeDesc")
  val eventTypeDesc: String,

  @Schema(example = "null", description = "Sub type (or reason) of scheduled event (as a code)")
  @JsonProperty("eventSubType")
  val eventSubType: String,

  @Schema(example = "null", description = "Description of scheduled event sub type")
  @JsonProperty("eventSubTypeDesc")
  val eventSubTypeDesc: String,

  @Valid
  @Schema(example = "null", description = "Date on which event occurs")
  @JsonProperty("eventDate")
  val eventDate: java.time.LocalDate,

  @Schema(example = "null", description = "Code identifying underlying source of event data")
  @JsonProperty("eventSource")
  val eventSource: String,

  @Schema(example = "null", description = "Activity id if any. Used to attend or pay an activity.")
  @JsonProperty("eventId")
  val eventId: Long? = null,

  @get:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
  @Schema(example = "2021-07-05T10:35:17", description = "Date and time at which event starts")
  @JsonProperty("startTime")
  val startTime: String? = null,

  @get:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
  @Schema(example = "2021-07-05T10:35:17", description = "Date and time at which event ends")
  @JsonProperty("endTime")
  val endTime: String? = null,

  @Schema(
    example = "null",
    description = "Location at which event takes place (could be an internal location, agency or external address).",
  )
  @JsonProperty("eventLocation")
  val eventLocation: String? = null,

  @Schema(example = "null", description = "Id of an internal event location")
  @JsonProperty("eventLocationId")
  val eventLocationId: Long? = null,

  @Schema(example = "WWI", description = "The agency ID for the booked internal location")
  @JsonProperty("agencyId")
  val agencyId: String? = null,

  @Schema(example = "null", description = "Source-specific code for the type or nature of the event")
  @JsonProperty("eventSourceCode")
  val eventSourceCode: String? = null,

  @Schema(example = "null", description = "Source-specific description for type or nature of the event")
  @JsonProperty("eventSourceDesc")
  val eventSourceDesc: String? = null,

  @Schema(
    example = "null",
    description = "Activity attendance, possible values are the codes in the 'PS_PA_OC' reference domain.",
  )
  @JsonProperty("eventOutcome")
  val eventOutcome: String? = null,

  @Schema(
    example = "null",
    description = "Activity performance, possible values are the codes in the 'PERFORMANCE' reference domain.",
  )
  @JsonProperty("performance")
  val performance: String? = null,

  @Schema(example = "null", description = "Activity no-pay reason.")
  @JsonProperty("outcomeComment")
  val outcomeComment: String? = null,

  @Schema(example = "null", description = "Activity paid flag.")
  @JsonProperty("paid")
  val paid: Boolean? = null,

  @Schema(example = "null", description = "Amount paid per activity session in pounds")
  @JsonProperty("payRate")
  val payRate: java.math.BigDecimal? = null,

  @Schema(example = "null", description = "The code for the activity location")
  @JsonProperty("locationCode")
  val locationCode: String? = null,

  @Schema(example = "null", description = "Staff member who created the appointment")
  @JsonProperty("createUserId")
  val createUserId: String? = null,
)
