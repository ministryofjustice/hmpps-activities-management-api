package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.UpdateCaseNoteUUIDResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PlannedDeallocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PlannedSuspensionRepository
import java.util.UUID

const val STATUS_COMPLETED = "COMPLETED"
const val STATUS_INCOMPLETE = "INCOMPLETE"
const val PAGE_SIZE = 5000

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
    updateCaseNoteUUIDForAttendanceHistories()
    updateCaseNoteUUIDForAllocations()
    updateCaseNoteUUIDForPlannedDeallocations()
    updateCaseNoteUUIDForPlannedSuspensions()

    return UpdateCaseNoteUUIDResponse(
      if (attendanceRepository.countByCaseNoteIdNotNullAndDpsCaseNoteIdNull() > 0) STATUS_INCOMPLETE else STATUS_COMPLETED,
      if (attendanceHistoryRepository.countByCaseNoteIdNotNullAndDpsCaseNoteIdNull() > 0) STATUS_INCOMPLETE else STATUS_COMPLETED,
      if (allocationRepository.countByDeallocationCaseNoteIdNotNullAndDeallocationDpsCaseNoteIdNull() > 0) STATUS_INCOMPLETE else STATUS_COMPLETED,
      if (plannedDeallocationRepository.countByCaseNoteIdNotNullAndDpsCaseNoteIdNull() > 0) STATUS_INCOMPLETE else STATUS_COMPLETED,
      if (plannedSuspensionRepository.countByCaseNoteIdNotNullAndDpsCaseNoteIdNull() > 0) STATUS_INCOMPLETE else STATUS_COMPLETED,
    )
  }

  private fun updateCaseNoteUUIDForAttendances() {
    var pageNumber = 0
    var currentPage = attendanceRepository.findAllCaseNoteIdToMigrate(currentPage(pageNumber))

    while (currentPage != null && currentPage.content.isNotEmpty()) {
      currentPage.forEach { attendance ->
        val caseNoteId = attendance.getCaseNoteId()
        val caseNote = caseNotesApiClient.getCaseNoteUUID(attendance.getPrisonerNumber(), caseNoteId)
        attendanceRepository.updateCaseNoteUUID(caseNoteId, UUID.fromString(caseNote.caseNoteId))
      }

      pageNumber++
      currentPage = attendanceRepository.findAllCaseNoteIdToMigrate(currentPage(pageNumber))
    }
  }

  private fun updateCaseNoteUUIDForAttendanceHistories() {
    var pageNumber = 0
    var currentPage = attendanceHistoryRepository.findAllCaseNoteIdToMigrate(currentPage(pageNumber))

    while (currentPage != null && currentPage.content.isNotEmpty()) {
      currentPage.forEach { attendanceHistory ->
        val caseNoteId = attendanceHistory.getCaseNoteId()
        val caseNote = caseNotesApiClient.getCaseNoteUUID(attendanceHistory.getPrisonerNumber(), caseNoteId)
        attendanceHistoryRepository.updateCaseNoteUUID(caseNoteId, UUID.fromString(caseNote.caseNoteId))
      }

      pageNumber++
      currentPage = attendanceHistoryRepository.findAllCaseNoteIdToMigrate(currentPage(pageNumber))
    }
  }

  private fun updateCaseNoteUUIDForAllocations() {
    var pageNumber = 0
    var currentPage = allocationRepository.findAllCaseNoteIdToMigrate(currentPage(pageNumber))

    while (currentPage != null && currentPage.content.isNotEmpty()) {
      currentPage.forEach { allocation ->
        val caseNoteId = allocation.getDeallocationCaseNoteId()
        val caseNote = caseNotesApiClient.getCaseNoteUUID(allocation.getPrisonerNumber(), caseNoteId)
        allocationRepository.updateCaseNoteUUID(caseNoteId, UUID.fromString(caseNote.caseNoteId))
      }

      pageNumber++
      currentPage = allocationRepository.findAllCaseNoteIdToMigrate(currentPage(pageNumber))
    }
  }

  private fun updateCaseNoteUUIDForPlannedDeallocations() {
    var pageNumber = 0
    var currentPage = plannedDeallocationRepository.findAllCaseNoteIdToMigrate(currentPage(pageNumber))

    while (currentPage != null && currentPage.content.isNotEmpty()) {
      currentPage.forEach { plannedDeallocation ->
        val caseNoteId = plannedDeallocation.getCaseNoteId()
        val caseNote = caseNotesApiClient.getCaseNoteUUID(plannedDeallocation.getPrisonerNumber(), caseNoteId)
        plannedDeallocationRepository.updateCaseNoteUUID(caseNoteId, UUID.fromString(caseNote.caseNoteId))
      }

      pageNumber++
      currentPage = plannedDeallocationRepository.findAllCaseNoteIdToMigrate(currentPage(pageNumber))
    }
  }

  private fun updateCaseNoteUUIDForPlannedSuspensions() {
    var pageNumber = 0
    var currentPage = plannedSuspensionRepository.findAllCaseNoteIdToMigrate(currentPage(pageNumber))

    while (currentPage != null && currentPage.content.isNotEmpty()) {
      currentPage.forEach { plannedSuspension ->
        val caseNoteId = plannedSuspension.getCaseNoteId()
        val caseNote = caseNotesApiClient.getCaseNoteUUID(plannedSuspension.getPrisonerNumber(), caseNoteId)
        plannedSuspensionRepository.updateCaseNoteUUID(caseNoteId, UUID.fromString(caseNote.caseNoteId))
      }

      pageNumber++
      currentPage = plannedSuspensionRepository.findAllCaseNoteIdToMigrate(currentPage(pageNumber))
    }
  }

  private fun currentPage(pageNumber: Int) = PageRequest.of(pageNumber, PAGE_SIZE)
}
