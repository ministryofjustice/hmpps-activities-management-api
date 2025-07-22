package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PlannedDeallocation
import java.util.UUID

interface MigratePlannedDeallocation {
  fun getPrisonerNumber(): String
  fun getCaseNoteId(): Long
}

interface PlannedDeallocationRepository : JpaRepository<PlannedDeallocation, Long> {
  @Query(
    value = """
      SELECT a.prisoner_number, pd.case_note_id
      FROM allocation a
      JOIN planned_deallocation pd ON a.allocation_id = pd.allocation_id
      WHERE pd.case_note_id IS NOT NULL
    """,
    nativeQuery = true,
  )
  fun findAllCaseNoteIdToMigrate(pageable: Pageable): Page<MigratePlannedDeallocation>

  @Modifying
  @Query(
    value = """
      UPDATE PlannedDeallocation pd
      SET pd.dpsCaseNoteId = :dpsCaseNoteId 
      WHERE pd.caseNoteId = :caseNoteId
    """,
  )
  fun updateCaseNoteUUID(caseNoteId: Long, dpsCaseNoteId: UUID)

  fun countByCaseNoteIdNotNullAndDpsCaseNoteIdNull(): Long
}
