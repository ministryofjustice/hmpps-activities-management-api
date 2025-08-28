package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageScheduledInstancesService

@Component
class CreateScheduledInstancesJob(
  private val service: ManageScheduledInstancesService,
  private val jobRunner: SafeJobRunner,
  featureSwitches: FeatureSwitches,
) {
  private val sqsEnabled = featureSwitches.isEnabled(Feature.JOBS_SQS_SCHEDULES_ENABLED)

  @Async("asyncExecutor")
  fun execute() {
    if (sqsEnabled) {
      jobRunner.runDistributedJob(JobType.SCHEDULES, service::sendCreateSchedulesEvents)
    } else {
      jobRunner.runJob(JobDefinition(JobType.SCHEDULES) { service.create() })
    }
  }
}
