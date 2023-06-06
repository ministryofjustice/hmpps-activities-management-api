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

  /**
   * The order in which the operations are supplied determines the order of execution of the operation.
   */
  @Async("asyncExecutor")
  fun execute(operations: List<AllocationOperation>) {
    if (operations.isEmpty()) {
      log.warn("No allocation operations specified")
    } else {
      log.info("The following allocation operations will be performed in sequence: $operations")
    }

    operations.distinct().forEach(service::allocations)
  }
}
