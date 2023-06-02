package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService

@Component
class ManageAllocationsJob(private val service: ManageAllocationsService) {

  @Async("asyncExecutor")
  fun execute() {
    service.allocations(AllocationOperation.STARTING_TODAY)
    service.allocations(AllocationOperation.DEALLOCATE_ENDING)
    service.allocations(AllocationOperation.DEALLOCATE_EXPIRING)
  }
}
