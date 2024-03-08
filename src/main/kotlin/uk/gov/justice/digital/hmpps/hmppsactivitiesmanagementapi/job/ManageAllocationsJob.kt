package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.daysAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.rangeTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AllocationOperation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAllocationsService
import java.time.LocalDate

@Component
class ManageAllocationsJob(
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val service: ManageAllocationsService,
  private val jobRunner: SafeJobRunner,
  @Value("\${jobs.deallocate-allocations-ending.days-start}") private val deallocateDaysStart: Int = 3,
) {

  @Async("asyncExecutor")
  fun execute(withActivate: Boolean = false, withDeallocateEnding: Boolean = false, withDeallocateExpiring: Boolean = false) {
    if (withActivate) {
      jobRunner.runJob(
        JobDefinition(JobType.ALLOCATE) {
          service.allocations(AllocationOperation.STARTING_TODAY)
        },
      )

      jobRunner.runJob(
        JobDefinition(jobType = JobType.START_SUSPENSIONS) {
          getRolledOutPrisonCodes().forEach { prisonCode ->
            service.suspendAllocationsDueToBeSuspended(prisonCode)
          }
        },
      )

      jobRunner.runJob(
        JobDefinition(jobType = JobType.END_SUSPENSIONS) {
          getRolledOutPrisonCodes().forEach { prisonCode ->
            service.unsuspendAllocationsDueToBeUnsuspended(prisonCode)
          }
        },
      )
    }

    if (withDeallocateEnding) {
      jobRunner.runJob(
        JobDefinition(jobType = JobType.DEALLOCATE_ENDING) {
          val startDate = deallocateDaysStart.daysAgo()
          val endDate = LocalDate.now()

          getRolledOutPrisonCodes().forEach { prisonCode ->
            startDate.rangeTo(endDate).forEach { service.endAllocationsDueToEnd(prisonCode, it) }
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
