package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceHistory

interface MigrateAttendanceHistory {
  fun getPrisonerNumber(): String
  fun getCaseNoteId(): Long
}

interface AttendanceHistoryRepository : JpaRepository<AttendanceHistory, Long> {
  @Query(
    value = """
      SELECT a.prisoner_number, ah.case_note_id
      FROM attendance a
      JOIN attendance_history ah ON a.attendance_id = ah.attendance_id
      WHERE ah.case_note_id IS NOT NULL
    """,
    nativeQuery = true,
  )
  fun findAllCaseNoteIdToMigrate(): List<MigrateAttendanceHistory>

  @Modifying
  @Query(
    value = """
      UPDATE AttendanceHistory ah
      SET ah.dpsCaseNoteId = :dpsCaseNoteId 
      WHERE ah.caseNoteId = :caseNoteId
    """,
  )
  fun updateCaseNoteUUID(caseNoteId: Long, dpsCaseNoteId: String)

  @Query(
    value = """
      SELECT ah FROM AttendanceHistory ah
      WHERE ah.caseNoteId IS NOT NULL
      AND ah.dpsCaseNoteId IS NULL
    """,
  )
  fun findRemainingCaseNoteIdToMigrate(): List<AttendanceHistory>
}
