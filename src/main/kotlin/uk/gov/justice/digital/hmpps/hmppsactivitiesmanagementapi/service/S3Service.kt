package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ObjectCannedAcl
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.sdk.kotlin.services.s3.model.ServerSideEncryption
import aws.smithy.kotlin.runtime.content.asByteStream
import aws.smithy.kotlin.runtime.content.writeToFile
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class S3Service(
  private val s3ClientAnalyticalPlatform: S3Client,
  @Value("\${aws.s3.ap.bucket}") private val awsApS3BucketName: String,
  @Value("\${aws.s3.ap.project}") private val awsApS3ProjectName: String,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

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
    report: File,
    fileName: String,
    tableName: String,
    bucketName: String = awsApS3BucketName,
  ): String {
    val extractionTimestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss'Z'")
      .withZone(ZoneOffset.UTC)
      .format(Instant.now())

    val filePath =
      "landing/$awsApS3ProjectName/data/database_name=activities_reports/table_name=$tableName/extraction_timestamp=$extractionTimestamp/$fileName"

    val request = PutObjectRequest {
      bucket = bucketName
      key = filePath
      body = report.asByteStream()
      serverSideEncryption = ServerSideEncryption.Aes256
      acl = ObjectCannedAcl.BucketOwnerFullControl
    }

    s3ClientAnalyticalPlatform.putObject(request)

    log.info("File uploaded to $filePath")

    return filePath
  }

  suspend fun getFileFromS3(
    fileName: String,
    tableName: String,
    bucketName: String = awsApS3BucketName,
  ): File {
    /* This method has initially been added to support testing and validation of uploads
       This is not currently suitable for usage in real applications in the system
     */
    val request = GetObjectRequest {
      bucket = bucketName
      key = fileName
    }

    // This function could ideally load the file into memory as saving to disk may not be necessary
    // If doing this though, care needs to be taken if the target file is of significant size
    val myFile = File("$tableName-pa-test.csv")

    runBlocking {
      s3ClientAnalyticalPlatform.getObject(
        request,
        {
          it.body?.writeToFile(myFile)
          println("saved file $fileName taken from aws s3")
        },
      )
    }

    return myFile
  }
}
