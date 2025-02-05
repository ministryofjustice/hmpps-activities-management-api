package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PurposefulActivityRepository
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.*
import java.util.stream.Stream
import kotlin.system.measureTimeMillis

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

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Executes appointment report and activity report against Activities DB
   * Then Uploads to the Analytical Platform S3 bucket for consumption by Prison Performance Reporting
   * weekOffset defaults to 1, which means it takes data up to the Saturday just passed.
   * increase the offset to go back in time more weeks, e.g. 3 = data up to third prior Saturday
   *
   * @param weekOffset How many weeks back to fetch data for, default 1 (most recent data set)
   */
  @Transactional(readOnly = true)
  fun executeAndUploadAllPurposefulActivityReports(
    weekOffset: Int = 1,
  ) {
    executeActivitiesReport(weekOffset)
    executeAppointmentsReport(weekOffset)
  }

  fun executeActivitiesReport(weekOffset: Int): String {
    log.info("Starting export of activity records")

    val dataStream = purposefulActivityRepository.getPurposefulActivityActivitiesReport(weekOffset)

    val csvReport = getResultsAsCsv(dataStream)

    val reportDate = getNthPreviousSunday(weekOffset)
    val csvActivitiesFileName = "activities_$reportDate.csv"

    var pushedFileKey: String

    runBlocking {
      pushedFileKey = s3Service.pushReportToAnalyticalPlatformS3(
        report = csvReport,
        tableName = purposefulActivityActivityTableName,
        fileName = csvActivitiesFileName,
      )
    }

    csvReport.delete()

    return pushedFileKey
  }

  fun executeAppointmentsReport(weekOffset: Int): String {
    log.info("Starting export of appointment records")

    val dataStream = purposefulActivityRepository.getPurposefulActivityAppointmentsReport(weekOffset)

    val csvReport = getResultsAsCsv(dataStream)

    val reportDate = getNthPreviousSunday(weekOffset)
    val csvAppointmentsFileName = "appointments_$reportDate.csv"

    var pushedFileKey: String
    // upload to s3 bucket
    runBlocking {
      pushedFileKey = s3Service.pushReportToAnalyticalPlatformS3(
        report = csvReport,
        tableName = purposefulActivityAppointmentsTableName,
        fileName = csvAppointmentsFileName,
      )
    }

    csvReport.delete()

    return pushedFileKey
  }

  internal fun getResultsAsCsv(results: Stream<*>): File {
    val file = File.createTempFile("exp", "csv")

    val writer = BufferedWriter(FileWriter(file), 32768)

    var rowNum = 0

    val elapsedMs = measureTimeMillis {
      writer.use {
        results.forEach {
          val row = it as Array<*>

          val rowString: String

          rowString = row.joinToString(",") { item ->
            formatCsvValue(item)
          }

          if (rowNum > 0) {
            writer.write("\n")
          }

          writer.write(rowString)

          rowNum += 1
        }
      }
    }

    log.info("$rowNum rows written to a temporary file in ${elapsedMs}ms")

    if (rowNum <= 1) {
      throw RuntimeException("Purposeful Activity Report data failed to find any relevant data")
    }

    return file
  }

  private fun formatCsvValue(item: Any?): String = when (item) {
    is String -> escapeCsvString(item)
    is Date -> "\"${formatDate(item)}\"" // Format and quote dates
    is Long, is Int, is Double -> item.toString()
    is Boolean -> item.toString() // Handle boolean values (true/false)
    null -> ""
    else -> "\"Unsupported type: ${item::class.simpleName}\""
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
