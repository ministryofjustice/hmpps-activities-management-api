package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

data class ActivityMigrateResponse(
  val prisonCode: String,
  val activityId: Long,
  val splitRegimeActivityId: Long? = null,
)
