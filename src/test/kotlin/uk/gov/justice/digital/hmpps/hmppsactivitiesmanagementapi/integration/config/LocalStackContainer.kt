package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.config

import org.slf4j.LoggerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.Transferable
import org.testcontainers.utility.DockerImageName
import java.io.IOException
import java.net.ServerSocket

object LocalStackContainer {
  private val log = LoggerFactory.getLogger(this::class.java)
  val instance by lazy { startLocalstackIfNotRunning() }

  fun setLocalStackProperties(localStackContainer: LocalStackContainer, registry: DynamicPropertyRegistry) {
    val localstackSnsUrl = localStackContainer.getEndpointOverride(LocalStackContainer.Service.SNS).toString()
    val localstacks3Url = localStackContainer.getEndpointOverride(LocalStackContainer.Service.S3).toString()
    val region = localStackContainer.region
    registry.add("hmpps.sqs.localstackUrl") { localstackSnsUrl }
    registry.add("hmpps.sqs.region") { region }
    registry.add("hmpps.s3.localstackUrl") { localstacks3Url }
    registry.add("aws.s3.ap.bucket") { "defaultbucket" }
  }

  private fun startLocalstackIfNotRunning(): LocalStackContainer? {
    if (localstackIsRunning()) return null
    val logConsumer = Slf4jLogConsumer(log).withPrefix("localstack")
    return LocalStackContainer(
      DockerImageName.parse("localstack/localstack").withTag("3"),
    ).apply {
      withCopyToContainer(
        Transferable.of(
          """
          #!/usr/bin/env bash
          awslocal s3 mb s3://defaultbucket
          """.trimIndent(),
          775,
        ),
        "/etc/localstack/init/ready.d/init-resources.sh",
      )
      withServices(LocalStackContainer.Service.SQS, LocalStackContainer.Service.SNS, LocalStackContainer.Service.S3)
      withEnv("DEFAULT_REGION", "eu-west-2")
      waitingFor(
        Wait.forLogMessage(".*Ready.*", 1),
      )
      start()
      followOutput(logConsumer)
    }
  }

  private fun localstackIsRunning(): Boolean =
    try {
      val serverSocket = ServerSocket(4566)
      serverSocket.localPort == 0
    } catch (e: IOException) {
      true
    }
}
