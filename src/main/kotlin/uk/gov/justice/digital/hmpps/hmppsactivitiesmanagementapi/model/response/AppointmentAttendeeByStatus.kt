package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "attendee and appointment details for a given status, ie not records")
data class AppointmentAttendeeByStatus(
  val prisonerNumber: String,
  val bookingId: Long,
  val appointmentId: Long,
  val appointmentAttendeeId: Long,
  val appointmentName: String,
  val startDate: LocalDate,
  val startTime: LocalTime,
  val endTime: LocalTime,
)
