package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAppointmentAttendeesService

@Component
class ManageAppointmentAttendeesJob(
  private val manageAppointmentAttendeesService: ManageAppointmentAttendeesService,
  private val jobRunner: SafeJobRunner,
  featureSwitches: FeatureSwitches,
) {

  private val sqsEnabled = featureSwitches.isEnabled(Feature.JOBS_SQS_MANAGE_APPOINTMENT_ATTENDEES_ENABLED)

  @Async("asyncExecutor")
  fun execute(daysAfterNow: Long) {
    if (sqsEnabled) {
      jobRunner.runDistributedJob(JobType.MANAGE_APPOINTMENT_ATTENDEES) { job ->
        manageAppointmentAttendeesService.sendEvents(job, daysAfterNow)
      }
    } else {
      jobRunner.runJob(JobDefinition(JobType.MANAGE_APPOINTMENT_ATTENDEES) { manageAppointmentAttendeesService.manageAttendees(daysAfterNow) })
    }
  }
}
