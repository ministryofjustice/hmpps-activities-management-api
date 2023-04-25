package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

data class BulkAppointment(

  val bulkAppointmentId: Long = 0,

  val appointments: List<Appointment>,
)
