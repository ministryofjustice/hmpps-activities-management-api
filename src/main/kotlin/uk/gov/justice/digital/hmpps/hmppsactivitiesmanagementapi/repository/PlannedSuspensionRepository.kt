package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PlannedSuspension
import java.util.UUID

interface MigratePlannedSuspension {
  fun getPrisonerNumber(): String
  fun getCaseNoteId(): Long
}

interface PlannedSuspensionRepository : JpaRepository<PlannedSuspension, Long> {
  @Query(
    value = """
      SELECT a.prisoner_number, ps.case_note_id
      FROM allocation a
      JOIN planned_suspension ps ON a.allocation_id = ps.allocation_id
      WHERE ps.case_note_id IS NOT NULL
    """,
    nativeQuery = true,
  )
  fun findAllCaseNoteIdToMigrate(pageable: Pageable): Page<MigratePlannedSuspension>

  @Modifying
  @Query(
    value = """
      UPDATE PlannedSuspension ps
      SET ps.dpsCaseNoteId = :dpsCaseNoteId 
      WHERE ps.caseNoteId = :caseNoteId
    """,
  )
  fun updateCaseNoteUUID(caseNoteId: Long, dpsCaseNoteId: UUID)

  fun countByCaseNoteIdNotNullAndDpsCaseNoteIdNull(): Long
}
