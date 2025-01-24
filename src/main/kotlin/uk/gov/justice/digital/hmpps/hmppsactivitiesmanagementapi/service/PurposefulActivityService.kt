package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PurposefulActivityRepository
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Date

@Service
class PurposefulActivityService(
  private val purposefulActivityRepository: PurposefulActivityRepository,
  private val s3Service: S3Service,
) {
  @Value("\${aws.s3.ap.bucket}")
  val awsApS3BucketName: String = "defaultbucket"

  @Value("\${aws.s3.ap.project}")
  private val awsApS3ProjectName: String = "test-ap-s3-project"

  val purposefulActivityActivityTableName = "activities"
  val purposefulActivityAppointmentsTableName = "appointments"

  /**
   * Executes appointment report and activity report against Activities DB
   * Then Uploads to the Analytical Platform S3 bucket for consumption by Prison Performance Reporting
   * weekOffset defaults to 1, which means it takes data up to the Saturday just passed.
   * increase the offset to go back in time more weeks, e.g. 3 = data up to third prior Saturday
   *
   * @param weekOffset How many weeks back to fetch data for, default 1 (most recent data set)
   */
  fun executeAndUploadAllPurposefulActivityReports(
    weekOffset: Int = 1,
  ) {
    executeActivitiesReport(weekOffset)
    executeAppointmentsReport(weekOffset)
  }

  fun executeActivitiesReport(weekOffset: Int): String {
    val activityData = purposefulActivityRepository.getPurposefulActivityActivitiesReport(weekOffset)

    if (activityData.isNullOrEmpty() || activityData.size <= 1) {
      throw RuntimeException("Purposeful Activity Report data failed to find any relevant activity data")
    }

    // convert to csv
    val csvActivitiesReport = getResultsAsCsv(activityData)

    val reportDate = getNthPreviousSunday(weekOffset)
    val csvActivitiesFileName = "activities_$reportDate.csv"

    var pushedFileKey: String
    // upload to s3 bucket
    runBlocking {
      pushedFileKey = s3Service.pushReportToAnalyticalPlatformS3(
        report = csvActivitiesReport.toByteArray(),
        tableName = purposefulActivityActivityTableName,
        fileName = csvActivitiesFileName,
      )
    }
    return pushedFileKey
  }

  fun executeAppointmentsReport(
    weekOffset: Int = 1,
  ): String {
    val appointmentData = purposefulActivityRepository.getPurposefulActivityAppointmentsReport(weekOffset)

    if (appointmentData.isNullOrEmpty() || appointmentData.size <= 1) {
      throw RuntimeException("Purposeful Activity Report data failed to find any relevant appointment data")
    }

    val csvAppointmentsReport = getResultsAsCsv(appointmentData)

    val reportDate = getNthPreviousSunday(weekOffset)
    val csvAppointmentsFileName = "appointments_$reportDate.csv"

    var pushedFileKey: String
    // upload to s3 bucket
    runBlocking {
      pushedFileKey = s3Service.pushReportToAnalyticalPlatformS3(
        report = csvAppointmentsReport.toByteArray(),
        tableName = purposefulActivityAppointmentsTableName,
        fileName = csvAppointmentsFileName,
      )
    }
    return pushedFileKey
  }

  internal fun getResultsAsCsv(results: MutableList<Any?>?): String {
    if (results.isNullOrEmpty()) throw IllegalArgumentException("Can't convert resultset to CSV because the result set is empty")

    val csvBuilder = StringBuilder(results.size * 100) // Estimated initial capacity

    results.forEach { row ->
      val myRealRow = row as? Array<Any?>
        ?: throw IllegalStateException("Expected an Array<Any?> but got ${row?.javaClass?.simpleName}")

      val rowString = myRealRow.joinToString(",") { item ->
        formatCsvValue(item)
      }
      csvBuilder.append(rowString).append("\n")
    }

    return csvBuilder.toString().trimEnd()
  }

  private fun formatCsvValue(item: Any?): String {
    return when (item) {
      is String -> escapeCsvString(item)
      is Date -> "\"${formatDate(item)}\"" // Format and quote dates
      is Long, is Int, is Double -> item.toString()
      is Boolean -> item.toString() // Handle boolean values (true/false)
      null -> ""
      else -> "\"Unsupported type: ${item::class.simpleName}\""
    }
  }

  private fun formatDate(date: Date): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss") // Customize the date format as needed
    return dateFormat.format(date)
  }

  private fun escapeCsvString(value: String): String {
    // Escape double quotes by doubling them
    val escapedValue = value.replace("\"", "\"\"")
    // Enclose the value in quotes
    return "\"$escapedValue\""
  }

  fun getNthPreviousSunday(n: Int): String {
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
