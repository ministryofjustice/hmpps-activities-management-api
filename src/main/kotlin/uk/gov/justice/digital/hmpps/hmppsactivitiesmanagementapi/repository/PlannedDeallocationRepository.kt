package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PlannedDeallocation

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
  fun findAllCaseNoteIdToMigrate(): List<MigratePlannedDeallocation>

  @Modifying
  @Query(
    value = """
      UPDATE PlannedDeallocation pd
      SET pd.dpsCaseNoteId = :dpsCaseNoteId 
      WHERE pd.caseNoteId = :caseNoteId
    """,
  )
  fun updateCaseNoteUUID(caseNoteId: Long, dpsCaseNoteId: String)

  @Query(
    value = """
      SELECT pd FROM PlannedDeallocation pd
      WHERE pd.caseNoteId IS NOT NULL
      AND pd.dpsCaseNoteId IS NULL
    """,
  )
  fun findRemainingCaseNoteIdToMigrate(): List<PlannedDeallocation>
}
