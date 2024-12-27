package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.BucketLocationConstraint
import aws.sdk.kotlin.services.s3.model.CreateBucketRequest
import aws.sdk.kotlin.services.s3.model.HeadBucketRequest
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

// An s3Client for usage with localstack, particularly when running in the circleci pipeline

@Configuration
@Profile("test")
class AWSLocalStackConfig {
  @Bean
  @Primary
  fun s3ClientAnalyticalPlatform(
    @Value("\${hmpps.s3.localstackUrl}") s3Url: String,
    @Value("\${hmpps.s3.region}") awsRegion: String,
    @Value("\${aws.s3.ap.bucket}") bucketName: String,
  ): S3Client {
    val s3Client = S3Client {
      credentialsProvider = StaticCredentialsProvider {
        accessKeyId = "test"
        secretAccessKey = "test"
      }
      region = awsRegion
      endpointUrl = aws.smithy.kotlin.runtime.net.url.Url.parse(s3Url)
      forcePathStyle = true
    }

    // Initialise the default bucket if doesn't exist
    runBlocking {
      val headBucketRequest = HeadBucketRequest {
        bucket = bucketName
      }

      try {
        s3Client.headBucket(headBucketRequest)
      } catch (e: Exception) {
        val request = CreateBucketRequest {
          bucket = bucketName
          createBucketConfiguration {
            locationConstraint = BucketLocationConstraint.fromValue(awsRegion)
          }
        }
        s3Client.createBucket(request)
      }
    }

    return s3Client
  }
}
