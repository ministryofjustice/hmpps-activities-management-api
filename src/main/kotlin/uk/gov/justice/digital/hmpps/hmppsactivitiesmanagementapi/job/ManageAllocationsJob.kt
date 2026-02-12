package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToEndService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToExpireService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageNewAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.SuspendAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.UnsuspendAllocationsService

@Component
class ManageAllocationsJob(
  private val manageAllocationsService: ManageAllocationsService,
  private val manageAllocationsDueToEndService: ManageAllocationsDueToEndService,
  private val manageAllocationsDueToExpireService: ManageAllocationsDueToExpireService,
  private val manageNewAllocationsService: ManageNewAllocationsService,
  private val suspendAllocationsService: SuspendAllocationsService,
  private val unsuspendAllocationsService: UnsuspendAllocationsService,
  private val jobRunner: SafeJobRunner,
  featureSwitches: FeatureSwitches,
) {
  private val sqsEnabledForDeallocateExpiring = featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATE_EXPIRING_ENABLED)
  private val sqsEnabledForActivateAllocations = featureSwitches.isEnabled(Feature.JOBS_SQS_ACTIVATE_ALLOCATIONS_ENABLED)

  @Async("asyncExecutor")
  fun execute(withActivate: Boolean = false, withDeallocateEnding: Boolean = false, withDeallocateExpiring: Boolean = false, withFixAutoSuspended: Boolean = false) {
    if (withActivate) {
      if (sqsEnabledForActivateAllocations) {
        jobRunner.runDistributedJob(JobType.ALLOCATE, manageNewAllocationsService::sendAllocationEvents)
      } else {
        jobRunner.runJobWithRetry(
          JobDefinition(JobType.ALLOCATE) { manageNewAllocationsService.allocations() },
        )

        jobRunner.runJobWithRetry(
          JobDefinition(JobType.START_SUSPENSIONS) { suspendAllocationsService.suspendAllocationsDueToBeSuspended() },
        )

        jobRunner.runJobWithRetry(
          JobDefinition(jobType = JobType.END_SUSPENSIONS) { unsuspendAllocationsService.unsuspendAllocationsDueToBeUnsuspended() },
        )
      }
    }

    if (withDeallocateEnding) {
      jobRunner.runDistributedJob(JobType.DEALLOCATE_ENDING, manageAllocationsDueToEndService::sendAllocationsDueToEndEvents)
    }

    if (withDeallocateExpiring) {
      if (sqsEnabledForDeallocateExpiring) {
        jobRunner.runDistributedJob(JobType.DEALLOCATE_EXPIRING, manageAllocationsDueToExpireService::sendAllocationsDueToExpireEvents)
      } else {
        jobRunner.runJobWithRetry(
          JobDefinition(jobType = JobType.DEALLOCATE_EXPIRING) { manageAllocationsDueToExpireService.deallocateAllocationsDueToExpire() },
        )
      }
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
