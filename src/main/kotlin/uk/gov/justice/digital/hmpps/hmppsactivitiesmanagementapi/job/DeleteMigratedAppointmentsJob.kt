package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MigrateAppointmentService
import java.time.LocalDate

@Component
class DeleteMigratedAppointmentsJob(
  private val jobRunner: SafeJobRunner,
  private val service: MigrateAppointmentService,
) {
  @Async("asyncExecutor")
  fun execute(prisonCode: String, startDate: LocalDate, categoryCode: String? = null) {
    jobRunner.runJob(
      JobDefinition(JobType.DELETE_MIGRATED_APPOINTMENTS) {
        service.deleteMigratedAppointments(prisonCode, startDate, categoryCode)
      },
    )
  }
}
