package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.LocalAuditRecord

data class LocalAuditSearchResults(
  val content: List<LocalAuditRecord>,
  val pageNumber: Int,
  val totalPages: Int,
)
