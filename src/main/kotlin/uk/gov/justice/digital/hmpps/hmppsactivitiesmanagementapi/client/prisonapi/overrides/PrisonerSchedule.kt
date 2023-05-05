package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

/**
 * TODO - document why we are overriding the generated version???
 */
data class PrisonerSchedule(

  @Schema(example = "null", description = "Offender number (e.g. NOMS Number)")
  @JsonProperty("offenderNo") val offenderNo: String,

  @Schema(
    example = "null",
    description = "The number which (uniquely) identifies the internal location associated with the Scheduled Event (Prisoner Schedule)"
  )
  @JsonProperty("locationId") val locationId: Long?,

  @Schema(example = "null", description = "Offender first name")
  @JsonProperty("firstName") val firstName: String,

  @Schema(example = "null", description = "Offender last name")
  @JsonProperty("lastName") val lastName: String,

  @Schema(example = "null", description = "Offender cell")
  @JsonProperty("cellLocation") val cellLocation: String?,

  @Schema(example = "null", description = "Event code")
  @JsonProperty("event") val event: String,

  @Schema(example = "null", description = "Event type, e.g. VISIT, APP, PRISON_ACT")
  @JsonProperty("eventType") val eventType: String?,

  @Schema(example = "null", description = "Description of event code")
  @JsonProperty("eventDescription") val eventDescription: String,

  @Schema(example = "null", description = "Location of the event")
  @JsonProperty("eventLocation") val eventLocation: String?,

  @Schema(example = "null", description = "The event's status. Includes 'CANC', meaning cancelled for 'VISIT'")
  @JsonProperty("eventStatus") val eventStatus: String?,

  @get:Size(min = 0, max = 4000)
  @Schema(example = "null", description = "Comment")
  @JsonProperty("comment") val comment: String?,

  @get:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
  @Schema(example = "2021-07-05T10:35:17", description = "Date and time at which event starts")
  @JsonProperty("startTime") val startTime: String,

  @Schema(example = "null", description = "Activity id if any. Used to attend or pay the event")
  @JsonProperty("eventId") val eventId: Long? = null,

  @Schema(example = "null", description = "Booking id for offender")
  @JsonProperty("bookingId") val bookingId: Long? = null,

  @Schema(example = "null", description = "Id of an internal event location")
  @JsonProperty("eventLocationId") val eventLocationId: Long? = null,

  @get:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
  @Schema(example = "2021-07-05T10:35:17", description = "Date and time at which event ends")
  @JsonProperty("endTime") val endTime: String? = null,

  @Schema(
    example = "null",
    description = "Attendance, possible values are the codes in the 'PS_PA_OC' reference domain"
  )
  @JsonProperty("eventOutcome") val eventOutcome: String? = null,

  @Schema(example = "null", description = "Possible values are the codes in the 'PERFORMANCE' reference domain")
  @JsonProperty("performance") val performance: String? = null,

  @Schema(example = "null", description = "No-pay reason")
  @JsonProperty("outcomeComment") val outcomeComment: String? = null,

  @Schema(example = "null", description = "Activity paid flag")
  @JsonProperty("paid") val paid: Boolean? = null,

  @Schema(example = "null", description = "Amount paid per activity session in pounds")
  @JsonProperty("payRate") val payRate: java.math.BigDecimal? = null,

  @Schema(example = "null", description = "Activity excluded flag")
  @JsonProperty("excluded") val excluded: Boolean? = null,

  @Schema(example = "null", description = "Activity time slot")
  @JsonProperty("timeSlot") val timeSlot: TimeSlot? = null,

  @Schema(example = "null", description = "The code for the activity location")
  @JsonProperty("locationCode") val locationCode: String? = null,

  @Schema(example = "null", description = "Event scheduled has been suspended")
  @JsonProperty("suspended") val suspended: Boolean? = null
) {
  /**
   * Activity time slot
   * Values: aM,pM,eD
   */
  enum class TimeSlot(val value: String) {

    @JsonProperty("AM")
    AM("AM"),
    @JsonProperty("PM")
    PM("PM"),
    @JsonProperty("ED")
    ED("ED")
  }
}
