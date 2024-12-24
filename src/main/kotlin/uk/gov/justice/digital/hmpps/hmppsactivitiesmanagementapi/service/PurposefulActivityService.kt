package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PurposefulActivityRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Service
class PurposefulActivityService(
  private val purposefulActivityRepository: PurposefulActivityRepository,
  private val s3Service: S3Service,
) {
  @Value("\${aws.s3.ap.bucket}")
  private val awsApS3BucketName: String = "defaultbucket"

  @Value("\${aws.s3.ap.project}")
  private val awsApS3ProjectName: String = "test-ap-s3-project"

  /**
   * Executes appointment report, activity report and prison rollout reports against Activities DB
   * Then Uploads to the Analytical Platform S3 bucket for consumption by Prison Performance Reporting
   * weekOffset defaults to 1, which means it takes data up to the Saturday just passed.
   * increase the offset to go back in time more weeks, e.g. 3 = data up to third prior Saturday
   *
   * @param weekOffset How many weeks back to fetch data for, default 1 (most recent data set)
   */
  fun executeAndUploadAllPurposefulActivityReports(
    weekOffset: Int = 1,
  ) {
    val activityData = purposefulActivityRepository.getPurposefulActivityActivitiesReport(weekOffset)
    val appointmentData = purposefulActivityRepository.getPurposefulActivityAppointmentsReport(weekOffset)
    val rolloutData = purposefulActivityRepository.getPurposefulActivityPrisonRolloutReport()

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
