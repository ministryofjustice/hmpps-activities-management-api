package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAppointmentAttendeesService

@Component
class ManageAppointmentAttendeesJob(
  private val manageAppointmentAttendeesService: ManageAppointmentAttendeesService,
  private val jobRunner: SafeJobRunner,
) {

  @Async("asyncExecutor")
  fun execute(daysAfterNow: Long) {
    jobRunner.runDistributedJob(JobType.MANAGE_APPOINTMENT_ATTENDEES) { job ->
      manageAppointmentAttendeesService.sendEvents(job, daysAfterNow)
    }
  }
}
