package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ActivitiesFixLocationsService

@Component
class ActivitiesFixLocationsJob(
  private val activitiesFixLocationsService: ActivitiesFixLocationsService,
  private val jobRunner: SafeJobRunner,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute() {
    log.info("Running activities fix locations job")

    jobRunner.runJob(
      JobDefinition(JobType.FIX_ACTIVITY_LOCATIONS) {
        activitiesFixLocationsService.fixActivityLocations()
      },
    )

    log.info("Finished running activities fix locations job")
  }
}
