package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.daysAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService

@Component
class ManageAllocationsJob(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val service: ManageAllocationsService,
  private val jobRunner: SafeJobRunner,
) {

  @Async("asyncExecutor")
  fun execute(withActivate: Boolean = false, withDeallocateEnding: Boolean = false, withDeallocateExpiring: Boolean = false) {
    if (withActivate) {
      jobRunner.runJob(
        JobDefinition(JobType.ALLOCATE) {
          service.allocations(AllocationOperation.STARTING_TODAY)
        },
      )
    }

    if (withDeallocateEnding) {
      jobRunner.runJob(
        JobDefinition(jobType = JobType.DEALLOCATE_ENDING) {
          getRolledOutPrisonCodes().forEach { prisonCode ->
            service.endAllocationsDueToEnd(prisonCode, 1.daysAgo())
          }
        },
      )
    }

    if (withDeallocateExpiring) {
      jobRunner.runJob(
        JobDefinition(jobType = JobType.DEALLOCATE_EXPIRING) {
          service.allocations(AllocationOperation.EXPIRING_TODAY)
        },
      )
    }
  }

  private fun getRolledOutPrisonCodes() =
    rolloutPrisonRepository.findAll().filter(RolloutPrison::isActivitiesRolledOut).map(RolloutPrison::code)
}
