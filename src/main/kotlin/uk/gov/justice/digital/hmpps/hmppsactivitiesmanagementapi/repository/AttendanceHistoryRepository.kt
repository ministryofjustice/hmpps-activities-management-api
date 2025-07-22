package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceHistory
import java.util.UUID

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
  fun findAllCaseNoteIdToMigrate(pageable: Pageable): Page<MigrateAttendanceHistory>

  @Modifying
  @Query(
    value = """
      UPDATE AttendanceHistory ah
      SET ah.dpsCaseNoteId = :dpsCaseNoteId 
      WHERE ah.caseNoteId = :caseNoteId
    """,
  )
  fun updateCaseNoteUUID(caseNoteId: Long, dpsCaseNoteId: UUID)

  fun countByCaseNoteIdNotNullAndDpsCaseNoteIdNull(): Long
}
