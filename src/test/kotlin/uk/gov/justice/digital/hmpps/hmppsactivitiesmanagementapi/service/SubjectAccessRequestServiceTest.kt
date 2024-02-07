package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.SarAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SubjectAccessRequestContent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.SarRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarAllocation as ModelSarAllocation

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
  )

  @Test
  fun `should return null when no content found`() {
    whenever(repository.findAllocationsBy("12345", TimeSource.today(), TimeSource.today())) doReturn emptyList()

    service.getContentFor("12345", null, null) isEqualTo null

    verify(repository).findAllocationsBy("12345", TimeSource.today(), TimeSource.today())
  }

  @Test
  fun `should return content when allocations found`() {
    whenever(repository.findAllocationsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())) doReturn listOf(sarAllocation)

    service.getContentFor("12345", TimeSource.yesterday(), TimeSource.tomorrow()) isEqualTo SubjectAccessRequestContent(
      prisonerNumber = sarAllocation.prisonerNumber,
      fromDate = TimeSource.yesterday(),
      toDate = TimeSource.tomorrow(),
      allocations = listOf(sarAllocation).map(::ModelSarAllocation),
    )

    verify(repository).findAllocationsBy("12345", TimeSource.yesterday(), TimeSource.tomorrow())
  }
}
