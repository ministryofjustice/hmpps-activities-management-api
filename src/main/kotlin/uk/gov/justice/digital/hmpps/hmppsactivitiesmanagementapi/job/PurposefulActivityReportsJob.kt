package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PurposefulActivityRepositoryCustom
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.S3Service

@Component
class PurposefulActivityReportsJob(
  private val jobRunner: SafeJobRunner,
  private val reportRepo: PurposefulActivityRepositoryCustom,
  private val s3Service: S3Service,
) {

  suspend fun execute(weekOffset: Int) {
    jobRunner.runJob(
      JobDefinition(JobType.PURPOSEFUL_ACTIVITY_REPORTS) {
        // execute prepared statements
        var activityData = reportRepo.getPurposefulActivityActivitiesReport(1)
        var appointmentData = reportRepo.getPurposefulActivityAppointmentsReport(1)

        // convert to csv
        var csvActivitiesReport = getResultsAsCsvByteStream(activityData)
        var csvAppointmentsReport = getResultsAsCsvByteStream(appointmentData)

        // upload to s3 bucket
        runBlocking {
          s3Service.pushReportToAnalyticalPlatformS3(csvActivitiesReport)
          s3Service.pushReportToAnalyticalPlatformS3(csvAppointmentsReport)
        }

        if (activityData.isNullOrEmpty()) {
          throw RuntimeException("Purposeful Activity Report data failed to find any relevant activity data")
        }

        if (appointmentData.isNullOrEmpty()) {
          throw RuntimeException("Purposeful Activity Report data failed to find any relevant appointment data")
        }
      },
    )
  }

  private fun getResultsAsCsvByteStream(results: MutableList<Any?>?): ByteArray {
    // Serialize the list to a CSV format string
    return buildString {
      results?.forEachIndexed { index, item ->
        if (index > 0) append("\n") // Add newline between records
        append(item.toString()) // Convert each item to string
      }
    }.toByteArray()
  }
}
