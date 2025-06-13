package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.updatesfromexternalsystems

data class UpdateFromExternalSystemEvent(
  val messageId: String,
  val eventType: String,
  val description: String? = null,
  val messageAttributes: Map<String, Any?> = emptyMap(),
  val who: String? = null,
)
