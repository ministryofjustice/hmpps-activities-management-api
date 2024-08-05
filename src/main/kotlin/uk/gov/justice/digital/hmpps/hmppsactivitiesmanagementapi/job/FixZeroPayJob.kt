package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.DataFixService
import java.time.LocalDate

@Component
class FixZeroPayJob(
  private val dataFixService: DataFixService,
  private val jobRunner: SafeJobRunner,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  val longTermSickActivityIdScheduleId = Pair(135L, 151L)
  val longTermSickPrisoners = listOf(
    Pair("A7175CH", LocalDate.now().plusDays(1)),
    Pair("A3903DM", LocalDate.now().plusDays(1)),
    Pair("A2539EW", LocalDate.now().plusDays(1)),
    Pair("A3084EX", LocalDate.now().plusDays(1)),
    Pair("A4778DA", LocalDate.now().plusDays(1)),
    Pair("A5617CQ", LocalDate.now().plusDays(1)),
  )
  val longTermSickPrisonerIdList = longTermSickPrisoners.map { it.first }

  @Async("asyncExecutor")
  fun execute(deallocate: Boolean = false, makeUnpaid: Boolean = false, allocate: Boolean = false) {
    log.info("Running fix paid to unpaid job with deallocate: $deallocate makeUnpaid: $makeUnpaid allocate: $allocate")

    if (deallocate) {
      jobRunner.runJob(
        JobDefinition(
          JobType.FIX_ZERO_PAY,
        ) { dataFixService.deallocate(longTermSickActivityIdScheduleId.second, longTermSickPrisonerIdList, LocalDate.now()) },
      )
    }

    if (makeUnpaid) {
      jobRunner.runJob(
        JobDefinition(
          JobType.FIX_ZERO_PAY,
        ) { dataFixService.makeUnpaid("RSI", longTermSickActivityIdScheduleId.first) },
      )
    }

    if (allocate) {
      jobRunner.runJob(
        JobDefinition(
          JobType.FIX_ZERO_PAY,
        ) { dataFixService.allocate(longTermSickActivityIdScheduleId.second, longTermSickPrisoners) },
      )
    }

    log.info("Finished running fix paid to unpaid job with deallocate: $deallocate makeUnpaid: $makeUnpaid allocate: $allocate")
  }
}
