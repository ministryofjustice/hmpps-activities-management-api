package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ManageAppointmentService
import java.time.LocalDate

@Component
class ManageAppointmentAttendeesJob(
  private val jobRunner: SafeJobRunner,
  private val service: ManageAppointmentService,
) {
  @Async("asyncExecutor")
  fun execute(daysBeforeNow: Long, daysAfterNow: Long) {
    jobRunner.runJob(
      JobDefinition(JobType.MANAGE_APPOINTMENT_ATTENDEES) {
        service.manageAppointmentAttendees(LocalDateRange(LocalDate.now().minusDays(daysBeforeNow), LocalDate.now().plusDays(daysAfterNow)))
      },
    )
  }
}
