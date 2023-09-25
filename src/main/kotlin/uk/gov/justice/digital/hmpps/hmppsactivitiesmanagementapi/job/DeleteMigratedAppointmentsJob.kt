package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import java.time.LocalDate

@Component
class DeleteMigratedAppointmentsJob(
  private val jobRunner: SafeJobRunner,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute(prisonCode: String, startDate: LocalDate, categoryCode: String? = null) {
    jobRunner.runJob(
      JobDefinition(JobType.DELETE_MIGRATED_APPOINTMENTS) {
        deleteMigratedAppointments()
      },
    )
  }

  private fun deleteMigratedAppointments() {

  }
}
