package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PurposefulActivityRepositoryCustom
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.S3Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Component
class PurposefulActivityReportsJob(
  private val jobRunner: SafeJobRunner,
  private val reportRepo: PurposefulActivityRepositoryCustom,
  private val s3Service: S3Service,
) {

  suspend fun execute(weekOffset: Int) {
    jobRunner.runJob(
      JobDefinition(JobType.PURPOSEFUL_ACTIVITY_REPORTS) {
        // execute report sql for each data type
        val activityData = reportRepo.getPurposefulActivityActivitiesReport(1)
        val appointmentData = reportRepo.getPurposefulActivityAppointmentsReport(1)
        val rolloutData = reportRepo.getPurposefulActivityPrisonRolloutReport()

        if (activityData.isNullOrEmpty()) {
          throw RuntimeException("Purposeful Activity Report data failed to find any relevant activity data")
        }

        if (appointmentData.isNullOrEmpty()) {
          throw RuntimeException("Purposeful Activity Report data failed to find any relevant appointment data")
        }

        if (rolloutData.isNullOrEmpty()) {
          throw RuntimeException("Purposeful Activity Report data failed to find any prison rollout data")
        }

        // convert to csv
        val csvActivitiesReport = getResultsAsCsvByteStream(activityData)
        val csvAppointmentsReport = getResultsAsCsvByteStream(appointmentData)
        val csvRolloutReport = getResultsAsCsvByteStream(rolloutData)

        val reportDate = getNthPreviousSunday(weekOffset)
        val csvActivitiesFileName = "activities_$reportDate.csv"
        val csvAppointmentsFileName = "appointments_$reportDate.csv"
        val csvRolloutFileName = "rollout_prisons_$reportDate.csv"

        // upload to s3 bucket
        runBlocking {
          s3Service.pushReportToAnalyticalPlatformS3(csvActivitiesReport, "activities", csvActivitiesFileName)
          s3Service.pushReportToAnalyticalPlatformS3(csvAppointmentsReport, "appointments", csvAppointmentsFileName)
          s3Service.pushReportToAnalyticalPlatformS3(csvRolloutReport, "rollout_prison", csvRolloutFileName)
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

  private fun getNthPreviousSunday(n: Int): String {
    // Get the current date
    val today = LocalDate.now()

    // Calculate the previous Sunday
    val previousSunday = today.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY))

    // Subtract (n-1) weeks from the previous Sunday
    val nthPreviousSunday = previousSunday.minusWeeks(n.toLong() - 1)

    // Format the date to "yyyyMMdd"
    val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
    return nthPreviousSunday.format(formatter)
  }
}
