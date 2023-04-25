package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import java.time.LocalDate
import java.time.LocalTime

data class BulkAppointmentsRequest(
  val prisonCode: String,
  val categoryCode: String,
  val appointmentDescription: String,
  val internalLocationId: Long,
  val inCell: Boolean,
  val startDate: LocalDate,
  val appointments: List<IndividualAppointment>,
)

data class IndividualAppointment(
  val prisonerNumber: String,
  val startTime: LocalTime,
  val endTime: LocalTime,
)
