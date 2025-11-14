package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesService

@Component
class CreateScheduledInstancesJob(
  private val service: ManageScheduledInstancesService,
  private val jobRunner: SafeJobRunner,
) {

  @Async("asyncExecutor")
  fun execute() {
    jobRunner.runDistributedJob(JobType.SCHEDULES, service::sendCreateSchedulesEvents)
  }
}
