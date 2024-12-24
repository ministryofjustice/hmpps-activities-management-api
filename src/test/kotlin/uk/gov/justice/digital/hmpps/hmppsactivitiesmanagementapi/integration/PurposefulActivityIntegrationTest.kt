package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import io.mockk.coEvery
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PurposefulActivityRepository

class PurposefulActivityIntegrationTest : IntegrationTestBase() {
  private val amazonS3: S3Client = mockk(relaxed = true)

  @Autowired
  private lateinit var purposefulActivityRepo: PurposefulActivityRepository

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
  fun `Purposeful Activity Repo runs activity report and data is validated`() {
    val activityData = purposefulActivityRepo.getPurposefulActivityActivitiesReport(1)
    assertThat(activityData).isNotNull
    assertThat(activityData).isNotEmpty
    assertThat(activityData).hasSize(2)

    assertThat(activityData).first().isNotNull

    // Assuming activityData has two arrays of objects
    val purposefulActivityRow1 = activityData?.first() as Array<*>
    val purposefulActivityRow2 = activityData[1] as Array<*>

    // Check some of the data is as expected for first attendance
    assertThat(purposefulActivityRow1[3]).isEqualTo("SAA_EDUCATION") // check activity category
    assertThat(purposefulActivityRow1[44]).isEqualTo("A11111A") // check prisoner number
    assertThat(purposefulActivityRow1[23]).isEqualTo("Maths AM")

    // Check some fields as expected for second attendance
    assertThat(purposefulActivityRow2).hasSize(55) // confirm total size
    assertThat(purposefulActivityRow2[13]).isEqualTo("Maths")
    assertThat(purposefulActivityRow2[8]).isEqualTo("Tier 1")
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
