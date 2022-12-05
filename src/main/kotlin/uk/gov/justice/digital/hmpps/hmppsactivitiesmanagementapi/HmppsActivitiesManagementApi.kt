package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HmppsActivitiesManagementApi

fun main(args: Array<String>) {
  runApplication<HmppsActivitiesManagementApi>(*args)
}
