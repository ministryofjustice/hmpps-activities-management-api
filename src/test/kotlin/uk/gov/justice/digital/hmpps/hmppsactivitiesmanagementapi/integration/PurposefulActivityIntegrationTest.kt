package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PurposefulActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PurposefulActivityService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.S3Service
import java.io.BufferedReader

class PurposefulActivityIntegrationTest : IntegrationTestBase() {
  private val amazonS3: S3Client = mockk(relaxed = true)

  @Autowired
  private lateinit var purposefulActivityRepo: PurposefulActivityRepository

  @Autowired
  private lateinit var purposefulActivityService: PurposefulActivityService

  @Autowired
  private lateinit var s3Service: S3Service

  private fun WebTestClient.executePurposefulActivityReportJob() {
    post()
      .uri("/job/purposeful-activity-reports")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isAccepted
  }

  @Sql(
    "classpath:test_data/seed-purposeful-activity-appts.sql",
    "classpath:test_data/seed-purposeful-activity-activities.sql",
  )
  @Test
  fun `Purposeful Activity Report is successfully run and uploaded to s3`() {
    waitForJobs({ webTestClient.executePurposefulActivityReportJob() })
  }

  @Sql(
    "classpath:test_data/seed-purposeful-activity-activities.sql",
  )
  @Test
  @Transactional
  fun `Purposeful Activity Report activities report is uploaded to s3, downloaded again and verified`() {
    val fileKey = purposefulActivityService.executeActivitiesReport(1)

    runBlocking {
      val file = s3Service.getFileFromS3(fileKey, "activities", purposefulActivityService.awsApS3BucketName)

      val bufferedReader: BufferedReader = file.bufferedReader()
      val csvContent = bufferedReader.use { it.readText() }

      assert(csvContent.isNotEmpty()) { "CSV content should not be empty" }

      val expectedHeader = "activity_activity_id,activity_prison_code,activity_activity_category_id"
      assert(csvContent.replace("\"", "").startsWith(expectedHeader)) { "CSV header does not match expected format" }

      val rows = csvContent.split("\n")
      assert(rows.size == 3) { "Output of this test should produce 3 rows" }
    }
  }

  @Sql(
    "classpath:test_data/seed-purposeful-activity-appts.sql",
  )
  @Test
  @Transactional
  fun `Purposeful Activity Report appointments report is uploaded to s3, downloaded again and verified`() {
    val fileKey = purposefulActivityService.executeAppointmentsReport(1)

    runBlocking {
      val file = s3Service.getFileFromS3(fileKey, "appointments", purposefulActivityService.awsApS3BucketName)

      val bufferedReader: BufferedReader = file.bufferedReader()
      val csvContent = bufferedReader.use { it.readText() }

      assert(csvContent.isNotEmpty()) { "CSV content should not be empty" }

      // Example of checking specific content
      val expectedHeader = "appointment_appointment_id,appointment_appointment_series_id,appointment_series_appointment_type,appointment_sequence_number"
      assert(csvContent.replace("\"", "").startsWith(expectedHeader)) { "CSV header does not match expected format" }

      val rows = csvContent.split("\n")
      assert(rows.size == 10) { "Output of this test should produce 3 rows" }
    }
  }

  @Sql(
    "classpath:test_data/seed-purposeful-activity-activities.sql",
  )
  @Test
  @Transactional
  fun `Purposeful Activity Repo runs activity report and data is validated`() {
    // This is really just a PurposefulActivityRepo test and could be moved to a dedicated
    // class for testing repos. At time of writing there was no general pattern for
    // creating test classes just for Repository classes.
    val activityData = purposefulActivityRepo.getPurposefulActivityActivitiesReport(1).toList()

    assertThat(activityData).isNotNull
    assertThat(activityData).isNotEmpty
    assertThat(activityData).hasSize(3)

    assertThat(activityData).first().isNotNull

    // Assuming activityData has two arrays of objects
    val purposefulActivityRow1 = activityData[1] as Array<*>
    val purposefulActivityRow2 = activityData[2] as Array<*>

    // Check some of the data is as expected for first attendance
    // These index numbers are going to be horrendous to maintain as the report changes
    assertThat(purposefulActivityRow1[3]).isEqualTo("SAA_EDUCATION") // check activity category
    assertThat(purposefulActivityRow1[34]).isEqualTo("A11111A") // check prisoner number
    assertThat(purposefulActivityRow1[22]).isEqualTo("Maths AM")

    // Check some fields as expected for second attendance
    assertThat(purposefulActivityRow2).hasSize(46) // confirm total columns as expected
    assertThat(purposefulActivityRow2[12]).isEqualTo("Maths")
    assertThat(purposefulActivityRow2[7]).isEqualTo("Tier 1")
  }

  @Sql(
    "classpath:test_data/seed-purposeful-activity-appts.sql",
  )
  @Test
  fun `Purposeful Activity Report job fails because activity data missing`() {
    val putObjectResponse = mockk<PutObjectResponse>()

    coEvery { amazonS3.putObject(any<PutObjectRequest>()) } returns putObjectResponse

    // The failure will mean there's zero completed jobs in the job table, so expect 0
    waitForJobs({ webTestClient.executePurposefulActivityReportJob() }, 0)
  }
}
