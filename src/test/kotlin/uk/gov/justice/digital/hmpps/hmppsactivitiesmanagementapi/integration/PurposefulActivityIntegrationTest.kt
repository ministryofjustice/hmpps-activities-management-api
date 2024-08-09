package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient

class PurposefulActivityIntegrationTest : IntegrationTestBase() {
  private val amazonS3: S3Client = mockk(relaxed = true)

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
