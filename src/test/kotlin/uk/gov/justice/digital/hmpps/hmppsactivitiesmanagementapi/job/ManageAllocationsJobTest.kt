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
  fun `activate allocation operation triggered`() {
    job.execute(withActivate = true)

    verify(offenderDeallocationService).allocations(AllocationOperation.STARTING_TODAY)
  }

  @Test
  fun `deallocate allocation operation triggered`() {
    job.execute(withDeallocate = true)

    verify(offenderDeallocationService).allocations(AllocationOperation.DEALLOCATE_ENDING)
    verify(offenderDeallocationService).allocations(AllocationOperation.DEALLOCATE_EXPIRING)
  }

  @Test
  fun `activate and deallocate allocation operations triggered`() {
    job.execute(withActivate = true, withDeallocate = true)

    verify(offenderDeallocationService).allocations(AllocationOperation.STARTING_TODAY)
    verify(offenderDeallocationService).allocations(AllocationOperation.DEALLOCATE_ENDING)
    verify(offenderDeallocationService).allocations(AllocationOperation.DEALLOCATE_EXPIRING)
  }
}
