package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentAttendeeService
import java.time.LocalDate

@Component
class ManageAppointmentAttendeesJob(
  private val jobRunner: SafeJobRunner,
  private val rolloutPrisonRepository: RolloutPrisonRepository,
  private val service: AppointmentAttendeeService,
) {
  @Async("asyncExecutor")
  fun execute(daysBeforeNow: Long, daysAfterNow: Long) {
    jobRunner.runJob(
      JobDefinition(JobType.MANAGE_APPOINTMENT_ATTENDEES) {
        // Do not check if prison has been enabled for appointments as it could still have migrated appointments requiring managing
        rolloutPrisonRepository.findAll().forEach {
          service.manageAppointmentAttendees(it.code, LocalDateRange(LocalDate.now().minusDays(daysBeforeNow), LocalDate.now().plusDays(daysAfterNow)))
        }
      },
    )
  }
}
