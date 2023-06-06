package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService

class ManageAllocationsJobTest {
  private val offenderDeallocationService: ManageAllocationsService = mock()
  private val job = ManageAllocationsJob(offenderDeallocationService)

  @Test
  fun `single allocation operation triggered`() {
    job.execute(listOf(AllocationOperation.DEALLOCATE_ENDING))

    verify(offenderDeallocationService).allocations(AllocationOperation.DEALLOCATE_ENDING)
  }

  @Test
  fun `all allocation operations triggered in order`() {
    job.execute(
      listOf(
        AllocationOperation.STARTING_TODAY,
        AllocationOperation.DEALLOCATE_ENDING,
        AllocationOperation.DEALLOCATE_EXPIRING,
      ),
    )

    offenderDeallocationService.inOrder {
      verify(offenderDeallocationService).allocations(AllocationOperation.STARTING_TODAY)
      verify(offenderDeallocationService).allocations(AllocationOperation.DEALLOCATE_ENDING)
      verify(offenderDeallocationService).allocations(AllocationOperation.DEALLOCATE_EXPIRING)
    }

    job.execute(
      listOf(
        AllocationOperation.DEALLOCATE_ENDING,
        AllocationOperation.DEALLOCATE_EXPIRING,
        AllocationOperation.STARTING_TODAY,
      ),
    )

    offenderDeallocationService.inOrder {
      verify(offenderDeallocationService).allocations(AllocationOperation.DEALLOCATE_ENDING)
      verify(offenderDeallocationService).allocations(AllocationOperation.DEALLOCATE_EXPIRING)
      verify(offenderDeallocationService).allocations(AllocationOperation.STARTING_TODAY)
    }
  }
}
