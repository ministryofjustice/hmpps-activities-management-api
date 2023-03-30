package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.LocalAuditRecord as ModelAuditRecord

@Entity
@Table(name = "local_audit")
data class LocalAuditRecord(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val localAuditId: Long = -1,

  val username: String,

  @Enumerated(EnumType.STRING)
  val auditType: AuditType,

  @Enumerated(EnumType.STRING)
  val detailType: AuditEventType,

  val recordedTime: LocalDateTime,

  val prisonCode: String,

  val prisonerNumber: String,

  val activityId: Long?,

  val activityScheduleId: Long?,

  val message: String,
) {

  fun toModel(): ModelAuditRecord {
    return ModelAuditRecord(
      localAuditId,
      username,
      auditType,
      detailType,
      recordedTime,
      prisonCode,
      prisonerNumber,
      activityId,
      activityScheduleId,
      message,
    )
  }
}
