package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.test.context.ActiveProfiles

@Configuration
@ActiveProfiles("test")
class AWSLocalStackConfig {
  @Bean
  @Primary
  fun s3ClientAnalyticalPlatform(
    @Value("\${hmpps.s3.localstackUrl}") s3Url: String,
    @Value("\${hmpps.s3.region}") awsRegion: String,
  ): S3Client = S3Client {
    credentialsProvider = StaticCredentialsProvider {
      accessKeyId = "test"
      secretAccessKey = "test"
    }
    region = awsRegion
    endpointUrl = aws.smithy.kotlin.runtime.net.url.Url.parse(s3Url)
  }
}
