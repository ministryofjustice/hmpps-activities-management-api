package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarWaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SubjectAccessRequestContent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.SarRepository
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarAllocation as ModelSarAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarAppointment as ModelSarAppointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarWaitingList as ModelSarWaitingList

class SubjectAccessRequestServiceTest {

  private val repository: SarRepository = mock()

  private val service = SubjectAccessRequestService(repository)

  private val sarAllocation = SarAllocation(
    allocationId = 1,
    prisonerNumber = "12345",
    prisonCode = MOORLAND_PRISON_CODE,
    prisonerStatus = PrisonerStatus.ACTIVE.name,
    startDate = TimeSource.yesterday(),
    endDate = null,
    activityId = 2,
    activitySummary = "Activity Summary",
    payBand = "Pay band 1",
    createdDate = TimeSource.yesterday(),
  )

  private val sarWaitingList = SarWaitingList(
    waitingListId = 2,
    prisonCode = PENTONVILLE_PRISON_CODE,
    prisonerNumber = "p89765",
    activitySummary = "Activity Summary WL",
    applicationDate = TimeSource.today(),
    originator = "GUIDANCE_STAFF",
    status = "PENDING",
    statusDate = TimeSource.tomorrow(),
    comments = "Like to try",
    createdDate = TimeSource.yesterday(),
  )

  private val sarAppointment = SarAppointment(
    appointmentId = 1,
    prisonCode = PENTONVILLE_PRISON_CODE,
    prisonerNumber = "p89765",
    categoryCode = "ACTI",
    startDate = TimeSource.tomorrow(),
    startTime = LocalTime.of(9, 30),
    endTime = LocalTime.of(11, 45),
    extraInformation = "Prayer session",
    attended = "Yes",
    createdDate = TimeSource.yesterday(),
  )

  @Test
  fun `should return null when no content found`() {
    whenever(repository.findAllocationsBy("12345", TimeSource.today(), TimeSource.today())) doReturn emptyList()
    whenever(repository.findWaitingListsBy("12345", TimeSource.today(), TimeSource.today())) doReturn emptyList()
    whenever(repository.findAllocationsBy("12345", TimeSource.today(), TimeSource.today())) doReturn emptyList()

    service.getContentFor("12345", null, null) isEqualTo null

    verify(repository).findAllocationsBy("12345", TimeSource.today(), TimeSource.today())
    verify(repository).findWaitingListsBy("12345", TimeSource.today(), TimeSource.today())
    verify(repository).findAppointmentsBy("12345", TimeSource.today(), TimeSource.today())
  }

  @Test
  fun `should return content when allocations found`() {
    whenever(repository.findAllocationsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())) doReturn listOf(sarAllocation)
    whenever(repository.findWaitingListsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())) doReturn emptyList()
    whenever(repository.findAppointmentsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())) doReturn emptyList()

    service.getContentFor("12345", TimeSource.yesterday(), TimeSource.tomorrow()) isEqualTo SubjectAccessRequestContent(
      prisonerNumber = sarAllocation.prisonerNumber,
      fromDate = TimeSource.yesterday(),
      toDate = TimeSource.tomorrow(),
      allocations = listOf(sarAllocation).map(::ModelSarAllocation),
      waitingListApplications = emptyList(),
      appointments = emptyList(),
    )

    verify(repository).findAllocationsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())
    verify(repository).findWaitingListsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())
    verify(repository).findAppointmentsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())
  }

  @Test
  fun `should return content when waiting lists found`() {
    whenever(repository.findAllocationsBy("12345", TimeSource.today(), TimeSource.tomorrow())) doReturn emptyList()
    whenever(repository.findWaitingListsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())) doReturn listOf(sarWaitingList)
    whenever(repository.findAppointmentsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())) doReturn emptyList()

    service.getContentFor("12345", TimeSource.yesterday(), TimeSource.tomorrow()) isEqualTo SubjectAccessRequestContent(
      prisonerNumber = sarAllocation.prisonerNumber,
      fromDate = TimeSource.yesterday(),
      toDate = TimeSource.tomorrow(),
      allocations = emptyList(),
      waitingListApplications = listOf(sarWaitingList).map(::ModelSarWaitingList),
      appointments = emptyList(),
    )

    verify(repository).findAllocationsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())
    verify(repository).findWaitingListsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())
    verify(repository).findAppointmentsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())
  }

  @Test
  fun `should return content when appointments found`() {
    whenever(repository.findAllocationsBy("12345", TimeSource.today(), TimeSource.tomorrow())) doReturn emptyList()
    whenever(repository.findWaitingListsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())) doReturn emptyList()
    whenever(repository.findAppointmentsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())) doReturn listOf(sarAppointment)

    service.getContentFor("12345", TimeSource.yesterday(), TimeSource.tomorrow()) isEqualTo SubjectAccessRequestContent(
      prisonerNumber = sarAllocation.prisonerNumber,
      fromDate = TimeSource.yesterday(),
      toDate = TimeSource.tomorrow(),
      allocations = emptyList(),
      waitingListApplications = emptyList(),
      appointments = listOf(sarAppointment).map(::ModelSarAppointment),
    )

    verify(repository).findAllocationsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())
    verify(repository).findWaitingListsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())
    verify(repository).findAppointmentsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())
  }

  @Test
  fun `should return content when allocations, waiting lists and appointments found`() {
    whenever(repository.findAllocationsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())) doReturn listOf(sarAllocation)
    whenever(repository.findWaitingListsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())) doReturn listOf(sarWaitingList)
    whenever(repository.findAppointmentsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())) doReturn listOf(sarAppointment)

    service.getContentFor("12345", TimeSource.yesterday(), TimeSource.tomorrow()) isEqualTo SubjectAccessRequestContent(
      prisonerNumber = sarAllocation.prisonerNumber,
      fromDate = TimeSource.yesterday(),
      toDate = TimeSource.tomorrow(),
      allocations = listOf(sarAllocation).map(::ModelSarAllocation),
      waitingListApplications = listOf(sarWaitingList).map(::ModelSarWaitingList),
      appointments = listOf(sarAppointment).map(::ModelSarAppointment),
    )

    verify(repository).findAllocationsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())
    verify(repository).findWaitingListsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())
    verify(repository).findAppointmentsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())
  }
}
