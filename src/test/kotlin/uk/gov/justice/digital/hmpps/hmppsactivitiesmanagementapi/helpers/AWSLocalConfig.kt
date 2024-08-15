package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectResponse
import io.mockk.coEvery
import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Configuration
@Profile("local")
class AWSLocalConfig {
  /*
  The purpose of this class is to satisfy application context loading dependencies
  When running the API service locally without also running localstack.
  It mocks away any calls to AWS services to ensure no accidental calls into
  the real world when running the service locally.
   */
  @Bean
  @Primary
  fun s3ClientAnalyticalPlatform(
  ): S3Client {
    val s3Client = mockk<S3Client>()
    val putObjectResponse = PutObjectResponse {
    }

    coEvery { s3Client.putObject(any()) } returns putObjectResponse
    return s3Client
  }
}
