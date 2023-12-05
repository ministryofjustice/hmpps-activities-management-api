package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

data class OffenderMergeDetails(val prisonCode: String, val newNumber: String, val oldNumber: String, val newBookingId: Int? = null)
