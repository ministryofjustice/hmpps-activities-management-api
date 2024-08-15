package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PurposefulActivityService

@Component
class PurposefulActivityReportsJob(
  private val jobRunner: SafeJobRunner,
  private val purposefulActivityService: PurposefulActivityService,
) {

  suspend fun execute(weekOffset: Int) {
    jobRunner.runJob(
      JobDefinition(JobType.PURPOSEFUL_ACTIVITY_REPORTS) {
        // execute report sql for each data type
        purposefulActivityService.executeAndUploadAllPurposefulActivityReports(weekOffset)
      },
    )
  }
}
