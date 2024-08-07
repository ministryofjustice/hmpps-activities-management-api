package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class S3Service(
  private val s3ClientAnalyticalPlatform: S3Client,
) {
  @Value("\${aws.s3.ap.bucket}")
  private val awsApS3BucketName: String = "defaultbucket"

  @Value("\${aws.s3.ap.project}")
  private val awsApS3ProjectName: String = "test-ap-s3-project"

  /**
   * Pushes a report to the Analytical Platform's S3 bucket with a specified naming convention.
   *
   * AP specify that for the register-my-data service, files must be pushed into folders matching this rigid structure:
   * landing/PROJECTNAME/data/database_name=NAME/table_name=TABLENAME/extraction_timestamp=YYYYMMDDHHMMSSZ/FILE.csv
   * The projectName has to correspond to the project names given in these yaml config files in register-my-data:
   *  https://github.com/ministryofjustice/register-my-data/tree/main/stacks
   *
   * First use-case for this (purposeful activity report) takes its data from a cross-table query,
   * so we don't need to use real table names, they can be made up, and that is the same for the databaseName too
   * tableName has to be unique for each different 'schema' i.e. each unique file format
   * As ever, it helps if any names used are meaningful and distinct enough in both activities n appts and AP
   *
   * @param report The report data as a ByteArray.
   * @param fileName The name of the file to be created in S3.
   * @param tableName A string which be included in the filepath as per the AP instructions
   * @param bucketName The name of the S3 bucket, for AP this is of the form moj-reg-<env> see the env vars in helm charts
   */
  suspend fun pushReportToAnalyticalPlatformS3(
    report: ByteArray,
    fileName: String,
    tableName: String,
    bucketName: String = awsApS3BucketName,
  ) {
    val extractionTimestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss'Z'")
      .withZone(ZoneOffset.UTC)
      .format(Instant.now())
    val filePath = "landing/$awsApS3ProjectName/data/database_name=activities_reports/table_name=$tableName/extraction_timestamp=$extractionTimestamp/$fileName"

    val request = PutObjectRequest {
      bucket = bucketName
      key = filePath
      body = ByteStream.fromBytes(report)
    }

    s3ClientAnalyticalPlatform.putObject(request)
  }
}
