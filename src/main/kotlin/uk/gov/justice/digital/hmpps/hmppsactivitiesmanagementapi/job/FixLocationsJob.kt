package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.FixActivitiesLocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.FixAppointmentSeriesLocationsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.FixAppointmentSetLocationsService

@Component
class FixLocationsJob(
  private val fixActivitiesLocationsService: FixActivitiesLocationsService,
  private val fixAppointmentSeriesLocationsService: FixAppointmentSeriesLocationsService,
  private val fixAppointmentSetLocationsService: FixAppointmentSetLocationsService,
  private val jobRunner: SafeJobRunner,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute() {
    log.info("Running fix locations job")

    jobRunner.runJob(
      JobDefinition(JobType.FIX_ACTIVITY_LOCATIONS) {
        fixActivitiesLocationsService.fixActivityLocations()
      },
    )

    jobRunner.runJob(
      JobDefinition(JobType.FIX_APPOINTMENT_SERIES_LOCATIONS) {
        fixAppointmentSeriesLocationsService.fixLocations()
      },
    )

    jobRunner.runJob(
      JobDefinition(JobType.FIX_APPOINTMENT_SET_LOCATIONS) {
        fixAppointmentSetLocationsService.fixLocations()
      },
    )

    log.info("Finished running fix locations job")
  }
}
