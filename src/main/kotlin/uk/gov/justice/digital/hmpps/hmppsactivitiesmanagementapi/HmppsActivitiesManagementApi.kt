package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HmppsActivitiesManagementApi {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun main(args: Array<String>) {
    val runtime = Runtime.getRuntime()

    val allocatedMemory = runtime.totalMemory() / 1024 / 1024
    val maxMemory = runtime.maxMemory() / 1024 / 1024

    log.info("Allocated memory: ${allocatedMemory}MB")
    log.info("Max memory: ${maxMemory}MB")

    runApplication<HmppsActivitiesManagementApi>(*args)
  }
}
