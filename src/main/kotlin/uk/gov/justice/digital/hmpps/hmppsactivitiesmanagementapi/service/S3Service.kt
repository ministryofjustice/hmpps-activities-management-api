package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class S3Service(
  private val s3ClientAnalyticalPlatform: S3Client,
) {
  @Value("\${aws.s3.ap.bucket}")
  lateinit var awsApS3BucketName: String

  suspend fun pushReportToAnalyticalPlatformS3(report: ByteArray, bucketName: String = awsApS3BucketName) {
    val request = PutObjectRequest {
      bucket = bucketName
      key = "landing/hmpps-activities-management-api-dev/purposeful_activity_test_report_file.csv" // dynamic filename needed
      body = ByteStream.fromBytes(report)
    }

    s3ClientAnalyticalPlatform.use { s3 ->
      s3.putObject(request)
    }
  }
}
