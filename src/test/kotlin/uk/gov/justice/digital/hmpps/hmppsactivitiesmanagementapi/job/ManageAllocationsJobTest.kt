package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService

class ManageAllocationsJobTest {
  private val offenderDeallocationService: ManageAllocationsService = mock()
  private val job = ManageAllocationsJob(offenderDeallocationService)

  @Test
  fun `attendance records creation triggered for today`() {
    job.execute()

    verify(offenderDeallocationService).allocations(AllocationOperation.DEALLOCATE_ENDING)
  }
}
