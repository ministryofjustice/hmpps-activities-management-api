package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import aws.sdk.kotlin.services.s3.model.S3Exception
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class S3ServiceTest {
  private val amazonS3: S3Client = mockk(relaxed = true)

  private val service = S3Service(amazonS3, "dummy bucket", "dummy project")

  @Test
  fun `Push a file to AP AWS S3`(): Unit = runBlocking {
    val mockBucketName = "aws-s3-ap-mock-bucket"
    val putObjectResponse = PutObjectResponse {
    }

    coEvery { amazonS3.putObject(any()) } returns putObjectResponse

    service.pushReportToAnalyticalPlatformS3("test-file".toByteArray(), "test-file.csv", "testTable", mockBucketName)
  }

  @Test
  fun `Push a file to AP AWS S3 with S3 error cascades error`(): Unit = runBlocking {
    val mockBucketName = "aws-s3-ap-mock-bucket"

    coEvery { amazonS3.putObject(any()) } throws S3Exception("S3 Exception")

    assertThrows<S3Exception> {
      runBlocking { service.pushReportToAnalyticalPlatformS3("test-file".toByteArray(), "test-file.csv", "testTable", mockBucketName) }
    }
  }
}
