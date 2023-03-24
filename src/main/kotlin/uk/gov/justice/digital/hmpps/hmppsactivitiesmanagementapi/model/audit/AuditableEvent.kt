package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit

import com.fasterxml.jackson.annotation.JsonIgnore
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.SecurityUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

open class AuditableEvent(
  @JsonIgnore
  val auditEventType: AuditEventType,
  @JsonIgnore
  val details: String,
  val createdAt: LocalDateTime,
) {

  val createdBy = SecurityUtils.getUserNameForLoggedInUser()

  override fun toString() = "$details. Event created on ${createdAt.toLocalDate()} " +
    "at ${createdAt.format(DateTimeFormatter.ofPattern("HH:mm:ss"))} by $createdBy."
}
