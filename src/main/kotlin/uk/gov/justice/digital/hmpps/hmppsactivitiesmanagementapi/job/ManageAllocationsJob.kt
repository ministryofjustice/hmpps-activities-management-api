package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService

@Component
class ManageAllocationsJob(
  private val service: ManageAllocationsService,
  private val jobRunner: SafeJobRunner,
) {

  @Async("asyncExecutor")
  fun execute(withActivate: Boolean = false, withDeallocate: Boolean = false) {
    if (withActivate) {
      jobRunner.runJob(
        JobDefinition(JobType.ALLOCATE) {
          service.allocations(AllocationOperation.STARTING_TODAY)
        },
      )
    }

    if (withDeallocate) {
      jobRunner.runJob(
        JobDefinition(jobType = JobType.DEALLOCATE_ENDING) {
          service.allocations(AllocationOperation.ENDING_TODAY)
        },
      )

      jobRunner.runJob(
        JobDefinition(jobType = JobType.DEALLOCATE_EXPIRING) {
          service.allocations(AllocationOperation.EXPIRING_TODAY)
        },
      )
    }
  }
}
