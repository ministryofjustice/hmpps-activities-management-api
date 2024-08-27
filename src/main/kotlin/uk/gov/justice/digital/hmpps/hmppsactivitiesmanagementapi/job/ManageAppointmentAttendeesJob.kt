package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentAttendeeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService

@Component
class ManageAppointmentAttendeesJob(
  private val jobRunner: SafeJobRunner,
  private val rolloutPrisonService: RolloutPrisonService,
  private val service: AppointmentAttendeeService,
) {
  @Async("asyncExecutor")
  fun execute(daysAfterNow: Long) {
    jobRunner.runJob(
      JobDefinition(JobType.MANAGE_APPOINTMENT_ATTENDEES) {
        // Do not check if prison has been enabled for appointments as it could still have migrated appointments requiring managing
        rolloutPrisonService.getRolloutPrisons().forEach {
          service.manageAppointmentAttendees(it.prisonCode, daysAfterNow)
        }
      },
    )
  }
}
