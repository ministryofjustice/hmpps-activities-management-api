package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.UpdateCaseNoteUUIDResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PlannedDeallocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PlannedSuspensionRepository

const val STATUS_COMPLETED = "COMPLETED"
const val STATUS_INCOMPLETE = "INCOMPLETE"

@Service
class MigrateCaseNotesUUIDService(
  private val attendanceRepository: AttendanceRepository,
  private val attendanceHistoryRepository: AttendanceHistoryRepository,
  private val allocationRepository: AllocationRepository,
  private val plannedDeallocationRepository: PlannedDeallocationRepository,
  private val plannedSuspensionRepository: PlannedSuspensionRepository,
  private val caseNotesApiClient: CaseNotesApiClient,
) {
  @Transactional
  fun updateCaseNoteUUID(): UpdateCaseNoteUUIDResponse {
    updateCaseNoteUUIDForAttendances()
    updateCaseNoteUUIDForAllocations()

    return UpdateCaseNoteUUIDResponse(
      if (attendanceRepository.findRemainingCaseNoteIdToMigrate().count() > 0) STATUS_INCOMPLETE else STATUS_COMPLETED,
      if (attendanceHistoryRepository.findRemainingCaseNoteIdToMigrate().count() > 0) STATUS_INCOMPLETE else STATUS_COMPLETED,
      if (allocationRepository.findRemainingCaseNoteIdToMigrate().count() > 0) STATUS_INCOMPLETE else STATUS_COMPLETED,
      if (plannedDeallocationRepository.findRemainingCaseNoteIdToMigrate().count() > 0) STATUS_INCOMPLETE else STATUS_COMPLETED,
      if (plannedSuspensionRepository.findRemainingCaseNoteIdToMigrate().count() > 0) STATUS_INCOMPLETE else STATUS_COMPLETED,
    )
  }

  private fun updateCaseNoteUUIDForAttendances() {
    attendanceRepository.findAllCaseNoteIdToMigrate()
      .forEach { attendance ->
        val caseNoteId = attendance.caseNoteId
        val caseNote = caseNotesApiClient.getCaseNoteUUID(attendance.prisonerNumber, caseNoteId!!)
        attendanceRepository.updateCaseNoteUUID(caseNoteId, caseNote.caseNoteId)
      }

    attendanceHistoryRepository.findAllCaseNoteIdToMigrate()
      .forEach { attendanceHistory ->
        val caseNoteId = attendanceHistory.getCaseNoteId()
        val caseNote = caseNotesApiClient.getCaseNoteUUID(attendanceHistory.getPrisonerNumber(), caseNoteId)
        attendanceHistoryRepository.updateCaseNoteUUID(caseNoteId, caseNote.caseNoteId)
      }
  }

  private fun updateCaseNoteUUIDForAllocations() {
    allocationRepository.findAllCaseNoteIdToMigrate()
      .forEach { allocation ->
        val caseNoteId = allocation.deallocationCaseNoteId
        val caseNote = caseNotesApiClient.getCaseNoteUUID(allocation.prisonerNumber, caseNoteId!!)
        allocationRepository.updateCaseNoteUUID(caseNoteId, caseNote.caseNoteId)
      }

    plannedDeallocationRepository.findAllCaseNoteIdToMigrate()
      .forEach { plannedDeallocation ->
        val caseNoteId = plannedDeallocation.getCaseNoteId()
        val caseNote = caseNotesApiClient.getCaseNoteUUID(plannedDeallocation.getPrisonerNumber(), caseNoteId)
        plannedDeallocationRepository.updateCaseNoteUUID(caseNoteId, caseNote.caseNoteId)
      }

    plannedSuspensionRepository.findAllCaseNoteIdToMigrate()
      .forEach { plannedSuspension ->
        val caseNoteId = plannedSuspension.getCaseNoteId()
        val caseNote = caseNotesApiClient.getCaseNoteUUID(plannedSuspension.getPrisonerNumber(), caseNoteId)
        plannedSuspensionRepository.updateCaseNoteUUID(caseNoteId, caseNote.caseNoteId)
      }
  }
}
