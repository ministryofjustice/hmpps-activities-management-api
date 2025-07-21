package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PlannedDeallocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PlannedSuspensionRepository
import java.time.LocalDate
import java.time.LocalDateTime

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

  val attendance = Attendance(
    attendanceId = 1,
    prisonerNumber = "ABC123",
    scheduledInstance = mock {
      on { isFuture(any()) } doReturn true
      on { sessionDate } doReturn LocalDate.now().plusDays(1)
    },
    caseNoteId = 111,
  )

  val caseNote = CaseNote(
    caseNoteId = "b7602cc8-e769-4cbb-8194-62d8e655992a",
    offenderIdentifier = "ABC123",
    type = "NEG",
    typeDescription = "Negative",
    subType = "sub type",
    subTypeDescription = "sub type description",
    source = "source",
    creationDateTime = LocalDateTime.now(),
    occurrenceDateTime = LocalDateTime.now(),
    authorName = "author",
    authorUserId = "author id",
    text = "Case Note Text",
    eventId = 1,
    sensitive = false,
  )

  @Test
  fun `update case note UUID`() {
    whenever(attendanceRepository.findAllCaseNoteIdToMigrate()).thenReturn(listOf(attendance))
    whenever(caseNotesApiClient.getCaseNoteUUID("ABC123", 111)).thenReturn(caseNote)
    whenever(attendanceRepository.findRemainingCaseNoteIdToMigrate()).thenReturn(emptyList())

    whenever(attendanceHistoryRepository.findAllCaseNoteIdToMigrate()).thenReturn(emptyList())
    whenever(attendanceHistoryRepository.findRemainingCaseNoteIdToMigrate()).thenReturn(emptyList())

    whenever(allocationRepository.findAllCaseNoteIdToMigrate()).thenReturn(emptyList())
    whenever(allocationRepository.findRemainingCaseNoteIdToMigrate()).thenReturn(emptyList())

    whenever(plannedDeallocationRepository.findAllCaseNoteIdToMigrate()).thenReturn(emptyList())
    whenever(plannedDeallocationRepository.findRemainingCaseNoteIdToMigrate()).thenReturn(emptyList())

    whenever(plannedSuspensionRepository.findAllCaseNoteIdToMigrate()).thenReturn(emptyList())
    whenever(plannedSuspensionRepository.findRemainingCaseNoteIdToMigrate()).thenReturn(emptyList())

    val response = service.updateCaseNoteUUID()

    verify(caseNotesApiClient).getCaseNoteUUID("ABC123", 111)
    verify(attendanceRepository).updateCaseNoteUUID(111, "b7602cc8-e769-4cbb-8194-62d8e655992a")

    assertThat(response.attendance).isEqualTo(STATUS_COMPLETED)
    assertThat(response.attendanceHistory).isEqualTo(STATUS_COMPLETED)
    assertThat(response.allocation).isEqualTo(STATUS_COMPLETED)
    assertThat(response.plannedSuspension).isEqualTo(STATUS_COMPLETED)
    assertThat(response.plannedDeallocation).isEqualTo(STATUS_COMPLETED)
  }
}
