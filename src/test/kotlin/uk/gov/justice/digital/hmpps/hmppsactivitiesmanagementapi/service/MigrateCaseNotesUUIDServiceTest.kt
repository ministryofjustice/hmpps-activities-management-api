package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PlannedDeallocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PlannedSuspensionRepository

class MigrateCaseNotesUUIDServiceTest {
  private val attendanceRepository: AttendanceRepository = mock()
  private val attendanceHistoryRepository: AttendanceHistoryRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val plannedDeallocationRepository: PlannedDeallocationRepository = mock()
  private val plannedSuspensionRepository: PlannedSuspensionRepository = mock()
  private val caseNotesApiClient: CaseNotesApiClient = mock()

  private val service = MigrateCaseNotesUUIDService(
    attendanceRepository,
    attendanceHistoryRepository,
    allocationRepository,
    plannedDeallocationRepository,
    plannedSuspensionRepository,
    caseNotesApiClient,
  )

  @Test
  fun `update case note UUID`() {
    val pageable = PageRequest.of(0, PAGE_SIZE)
    whenever(attendanceRepository.findAllCaseNoteIdToMigrate(pageable)).thenReturn(PageImpl(emptyList()))
    whenever(attendanceRepository.countByCaseNoteIdNotNullAndDpsCaseNoteIdNull()).thenReturn(0)

    whenever(attendanceHistoryRepository.findAllCaseNoteIdToMigrate(pageable)).thenReturn(PageImpl(emptyList()))
    whenever(attendanceHistoryRepository.countByCaseNoteIdNotNullAndDpsCaseNoteIdNull()).thenReturn(0)

    whenever(allocationRepository.findAllCaseNoteIdToMigrate(pageable)).thenReturn(PageImpl(emptyList()))
    whenever(allocationRepository.countByDeallocationCaseNoteIdNotNullAndDeallocationDpsCaseNoteIdNull()).thenReturn(0)

    whenever(plannedDeallocationRepository.findAllCaseNoteIdToMigrate(pageable)).thenReturn(PageImpl(emptyList()))
    whenever(plannedDeallocationRepository.countByCaseNoteIdNotNullAndDpsCaseNoteIdNull()).thenReturn(0)

    whenever(plannedSuspensionRepository.findAllCaseNoteIdToMigrate(pageable)).thenReturn(PageImpl(emptyList()))
    whenever(plannedSuspensionRepository.countByCaseNoteIdNotNullAndDpsCaseNoteIdNull()).thenReturn(0)

    val response = service.updateCaseNoteUUID()

    with(response) {
      assertThat(attendance).isEqualTo(STATUS_COMPLETED)
      assertThat(attendanceHistory).isEqualTo(STATUS_COMPLETED)
      assertThat(allocation).isEqualTo(STATUS_COMPLETED)
      assertThat(plannedSuspension).isEqualTo(STATUS_COMPLETED)
      assertThat(plannedDeallocation).isEqualTo(STATUS_COMPLETED)
    }
  }
}
