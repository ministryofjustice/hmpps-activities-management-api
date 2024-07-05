package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import aws.sdk.kotlin.services.s3.S3Client
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!test && !local")
class AWSConfig() {
  @Bean
  fun s3ClientAnalyticalPlatform(
    @Value("\${aws.s3.ap.region}") awsRegion: String,
  ): S3Client = S3Client {
    region = awsRegion
  }
}
