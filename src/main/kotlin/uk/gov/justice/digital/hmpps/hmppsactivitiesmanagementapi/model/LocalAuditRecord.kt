package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import java.time.LocalDateTime

data class LocalAuditRecord(

  val localAuditId: Long = -1,

  val username: String,

  val auditType: AuditType,

  val auditEventType: AuditEventType,

  val recordedTime: LocalDateTime,

  val prisonCode: String,

  val prisonerNumber: String? = null,

  val activityId: Long? = null,

  val activityScheduleId: Long? = null,

  val message: String,
)
