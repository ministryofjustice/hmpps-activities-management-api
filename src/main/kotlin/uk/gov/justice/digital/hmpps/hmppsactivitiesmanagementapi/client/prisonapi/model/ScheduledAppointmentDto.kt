package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern

/**
 * Scheduled appointment
 * @param id Appointment id
 * @param offenderNo Offender number (e.g. NOMS Number)
 * @param firstName Offender first name
 * @param lastName Offender last name
 * @param date Date the appointment is scheduled
 * @param startTime Date and time at which appointment starts
 * @param endTime Date and time at which appointment ends
 * @param appointmentTypeDescription Description of appointment type
 * @param appointmentTypeCode Appointment code
 * @param locationDescription Description of location the appointment is held
 * @param locationId Id of location the appointment is held
 * @param createUserId Staff member who created the appointment
 * @param agencyId Agency the appointment belongs to
 */
data class ScheduledAppointmentDto(

  @Schema(example = "null", description = "Appointment id")
  @JsonProperty("id")
  val id: Long? = null,

  @Schema(example = "null", description = "Offender number (e.g. NOMS Number)")
  @JsonProperty("offenderNo")
  val offenderNo: String? = null,

  @Schema(example = "null", description = "Offender first name")
  @JsonProperty("firstName")
  val firstName: String? = null,

  @Schema(example = "null", description = "Offender last name")
  @JsonProperty("lastName")
  val lastName: String? = null,

  @Valid
  @Schema(example = "null", description = "Date the appointment is scheduled")
  @JsonProperty("date")
  val date: java.time.LocalDate? = null,

  @get:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
  @Schema(example = "2021-07-05T10:35:17", description = "Date and time at which appointment starts")
  @JsonProperty("startTime")
  val startTime: String? = null,

  @get:Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
  @Schema(example = "2021-07-05T10:35:17", description = "Date and time at which appointment ends")
  @JsonProperty("endTime")
  val endTime: String? = null,

  @Schema(example = "null", description = "Description of appointment type")
  @JsonProperty("appointmentTypeDescription")
  val appointmentTypeDescription: String? = null,

  @Schema(example = "null", description = "Appointment code")
  @JsonProperty("appointmentTypeCode")
  val appointmentTypeCode: String? = null,

  @Schema(example = "null", description = "Description of location the appointment is held")
  @JsonProperty("locationDescription")
  val locationDescription: String? = null,

  @Schema(example = "null", description = "Id of location the appointment is held")
  @JsonProperty("locationId")
  val locationId: Long? = null,

  @Schema(example = "null", description = "Staff member who created the appointment")
  @JsonProperty("createUserId")
  val createUserId: String? = null,

  @Schema(example = "null", description = "Agency the appointment belongs to")
  @JsonProperty("agencyId")
  val agencyId: String? = null,
)
