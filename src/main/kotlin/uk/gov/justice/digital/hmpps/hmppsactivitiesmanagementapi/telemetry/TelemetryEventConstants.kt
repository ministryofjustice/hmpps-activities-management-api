package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry

const val ACKNOWLEDGED_BY_KEY = "acknowledgedBy"
const val ACKNOWLEDGED_TIME_KEY = "acknowledgedTime"
const val ACTIVITY_NAME_KEY = "activityName"
const val EVENT_REVIEW_IDS_KEY = "eventReviewIds"
const val NUMBER_OF_RESULTS_KEY = "numberOfResults"
const val PRISON_NAME_KEY = "prisonName"
const val PRISONER_NUMBER_KEY = "prisonerNumber"

fun metricsMap() = mapOf(
  NUMBER_OF_RESULTS_KEY to 1.0,
)
