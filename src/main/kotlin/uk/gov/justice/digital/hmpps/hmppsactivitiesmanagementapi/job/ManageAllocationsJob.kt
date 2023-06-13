package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService

@Component
class ManageAllocationsJob(
  private val service: ManageAllocationsService,
  private val safeJobRunner: SafeJobRunner,
) {

  @Async("asyncExecutor")
  fun execute(withActivate: Boolean = false, withDeallocate: Boolean = false) {
    if (withActivate) {
      safeJobRunner.runSafe(
        JobDefinition(JobType.ALLOCATION) {
          service.allocations(AllocationOperation.STARTING_TODAY)
        },
      )
    }

    if (withDeallocate) {
      safeJobRunner.runSafe(
        JobDefinition(jobType = JobType.DEALLOCATION) {
          service.allocations(AllocationOperation.DEALLOCATE_ENDING)
        },
        JobDefinition(jobType = JobType.DEALLOCATION) {
          service.allocations(AllocationOperation.DEALLOCATE_EXPIRING)
        },
      )
    }
  }
}
