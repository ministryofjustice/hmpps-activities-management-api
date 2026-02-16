package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToEndService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToExpireService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageNewAllocationsService

@Component
class ManageAllocationsJob(
  private val manageAllocationsService: ManageAllocationsService,
  private val manageAllocationsDueToEndService: ManageAllocationsDueToEndService,
  private val manageAllocationsDueToExpireService: ManageAllocationsDueToExpireService,
  private val manageNewAllocationsService: ManageNewAllocationsService,
  private val jobRunner: SafeJobRunner,
) {

  @Async("asyncExecutor")
  fun execute(withActivate: Boolean = false, withDeallocateEnding: Boolean = false, withDeallocateExpiring: Boolean = false, withFixAutoSuspended: Boolean = false) {
    if (withActivate) {
      jobRunner.runDistributedJob(JobType.ALLOCATE, manageNewAllocationsService::sendAllocationEvents)
    }

    if (withDeallocateEnding) {
      jobRunner.runDistributedJob(JobType.DEALLOCATE_ENDING, manageAllocationsDueToEndService::sendAllocationsDueToEndEvents)
    }

    if (withDeallocateExpiring) {
      jobRunner.runDistributedJob(JobType.DEALLOCATE_EXPIRING, manageAllocationsDueToExpireService::sendAllocationsDueToExpireEvents)
    }

    if (withFixAutoSuspended) {
      jobRunner.runJobWithRetry(
        JobDefinition(jobType = JobType.FIX_STUCK_AUTO_SUSPENDED) {
          manageAllocationsService.fixPrisonersIncorrectlyAutoSuspended()
        },
      )
    }
  }
}
