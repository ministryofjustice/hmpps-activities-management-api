package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

data class AppointmentOccurrenceCancelRequest(
  val cancellationReasonId: Long,
  val applyTo: ApplyTo,
)
