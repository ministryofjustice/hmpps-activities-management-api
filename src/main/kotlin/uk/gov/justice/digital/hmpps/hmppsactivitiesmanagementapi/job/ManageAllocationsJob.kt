package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsDueToEndService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService

@Component
class ManageAllocationsJob(
  private val rolloutPrisonService: RolloutPrisonService,
  private val service: ManageAllocationsService,
  private val manageAllocationsDueToEndService: ManageAllocationsDueToEndService,
  private val jobRunner: SafeJobRunner,
  featureSwitches: FeatureSwitches,
) {
  private val sqsEnabledForDeallocateEnding = featureSwitches.isEnabled(Feature.JOBS_SQS_DEALLOCATED_ENDING_ENABLED)

  @Async("asyncExecutor")
  fun execute(withActivate: Boolean = false, withDeallocateEnding: Boolean = false, withDeallocateExpiring: Boolean = false, withFixAutoSuspended: Boolean = false) {
    if (withActivate) {
      jobRunner.runJobWithRetry(
        JobDefinition(JobType.ALLOCATE) {
          service.allocations(AllocationOperation.STARTING_TODAY)
        },
      )

      jobRunner.runJobWithRetry(
        JobDefinition(jobType = JobType.START_SUSPENSIONS) {
          getRolledOutPrisonCodes().forEach { prisonCode ->
            service.suspendAllocationsDueToBeSuspended(prisonCode)
          }
        },
      )

      jobRunner.runJobWithRetry(
        JobDefinition(jobType = JobType.END_SUSPENSIONS) {
          getRolledOutPrisonCodes().forEach { prisonCode ->
            service.unsuspendAllocationsDueToBeUnsuspended(prisonCode)
          }
        },
      )
    }

    if (withDeallocateEnding) {
      if (sqsEnabledForDeallocateEnding) {
        jobRunner.runDistributedJob(JobType.DEALLOCATE_ENDING, manageAllocationsDueToEndService::sendAllocationsDueToEndEvents)
      } else {
        jobRunner.runJobWithRetry(
          JobDefinition(jobType = JobType.DEALLOCATE_ENDING) { manageAllocationsDueToEndService.endAllocationsDueToEnd() },
        )
      }
    }

    if (withDeallocateExpiring) {
      jobRunner.runJobWithRetry(
        JobDefinition(jobType = JobType.DEALLOCATE_EXPIRING) {
          service.allocations(AllocationOperation.EXPIRING_TODAY)
        },
      )
    }

    if (withFixAutoSuspended) {
      jobRunner.runJobWithRetry(
        JobDefinition(jobType = JobType.FIX_STUCK_AUTO_SUSPENDED) {
          service.fixPrisonersIncorrectlyAutoSuspended()
        },
      )
    }
  }

  private fun getRolledOutPrisonCodes(): List<String> = rolloutPrisonService.getRolloutPrisons().map { it.prisonCode }
}
