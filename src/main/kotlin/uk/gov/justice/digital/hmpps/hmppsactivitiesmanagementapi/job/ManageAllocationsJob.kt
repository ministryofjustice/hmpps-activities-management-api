package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OffenderDeallocationService

@Component
class ManageAllocationsJob(private val offenderDeallocationService: OffenderDeallocationService) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute() {
    log.info("Deallocating offenders from activities ending today")
    offenderDeallocationService.deallocateOffendersWhenEndDatesReached()
  }
}
