package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse

class PurposefulActivityIntegrationTest : IntegrationTestBase() {
  private val amazonS3: S3Client = mockk(relaxed = true)

  @Sql(
    "classpath:test_data/seed-purposeful-activity-appts.sql",
    "classpath:test_data/seed-purposeful-activity-activities.sql",
  )
  @Test
  fun `Purposeful Activity Report successfully uploads reports`() {
    val putObjectResponse = mockk<PutObjectResponse>()

    coEvery { amazonS3.putObject(any<PutObjectRequest>()) } returns putObjectResponse

    webTestClient.post()
      .uri("/job/purposeful-activity-reports")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().isAccepted
  }

  @Sql(
    "classpath:test_data/seed-purposeful-activity-appts.sql",
  )
  @Test
  fun `Purposeful Activity Report throws Exception because activity data not returned`() {
    val putObjectResponse = mockk<PutObjectResponse>()

    coEvery { amazonS3.putObject(any<PutObjectRequest>()) } returns putObjectResponse

    webTestClient.post()
      .uri("/job/purposeful-activity-reports")
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody
  }
}
