package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import java.time.LocalDateTime

@Repository
interface AuditRepository : JpaRepository<LocalAuditRecord, Long> {
  fun findByPrisonCodeAndPrisonerNumber(prisonCode: String, prisonerNumber: String): List<LocalAuditRecord>

  @Query(
    """ 
    FROM LocalAuditRecord ar 
    WHERE (:prisonCode IS NULL OR ar.prisonCode = :prisonCode) 
    AND (:prisonerNumber IS NULL OR ar.prisonerNumber = :prisonerNumber) 
    AND (:username IS NULL OR ar.username = :username)
     AND (:auditType IS NULL OR ar.auditType = :auditType)
     AND (:auditEventType IS NULL OR ar.detailType = :auditEventType)
     AND (cast(:startTime as timestamp) IS NULL OR ar.recordedTime >= :startTime)
     AND (cast(:endTime as timestamp) IS NULL OR ar.recordedTime <= :endTime)
     AND (:activityId IS NULL OR ar.activityId = :activityId)
     AND (:scheduleId IS NULL OR ar.activityScheduleId = :scheduleId)
    """,
  )
  fun searchRecords(
    prisonCode: String? = null,
    prisonerNumber: String? = null,
    username: String? = null,
    auditType: AuditType? = null,
    auditEventType: AuditEventType? = null,
    startTime: LocalDateTime? = null,
    endTime: LocalDateTime? = null,
    activityId: Long? = null,
    scheduleId: Long? = null,
    pageable: Pageable,
  ): Page<LocalAuditRecord>

  @Query(value = "UPDATE LocalAuditRecord l SET l.prisonerNumber = :newNumber WHERE l.prisonerNumber = :oldNumber")
  @Modifying
  fun mergeOffender(oldNumber: String, newNumber: String)
}
