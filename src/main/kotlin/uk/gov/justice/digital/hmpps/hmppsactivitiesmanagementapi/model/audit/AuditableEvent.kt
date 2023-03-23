package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

open class AuditableEvent(
  val createdAt: LocalDateTime,
  val createdBy: String,
) {
  override fun toString() = "Event created on ${createdAt.toLocalDate()} " +
    "at ${createdAt.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} by $createdBy."
}
