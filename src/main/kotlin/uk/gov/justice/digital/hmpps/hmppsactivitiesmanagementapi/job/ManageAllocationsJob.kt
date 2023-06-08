package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService

@Component
class ManageAllocationsJob(private val service: ManageAllocationsService) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute(withActivate: Boolean = false, withDeallocate: Boolean = false) {
    if (withActivate) {
      service.allocations(AllocationOperation.STARTING_TODAY)
    }

    if (withDeallocate) {
      service.allocations(AllocationOperation.DEALLOCATE_ENDING)
      service.allocations(AllocationOperation.DEALLOCATE_EXPIRING)
    }
  }
}
