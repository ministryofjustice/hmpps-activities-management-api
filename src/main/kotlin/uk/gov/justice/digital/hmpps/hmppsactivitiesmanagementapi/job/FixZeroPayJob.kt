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

  @Async("asyncExecutor")
  fun execute(deallocate: Boolean = false, makeUnpaid: Boolean = false, allocate: Boolean = false) {
    log.info("Running fix paid to unpaid job with deallocate: $deallocate makeUnpaid: $makeUnpaid allocate: $allocate")

    val retiredActivityIdScheduleId = Pair(119L, 129L)
    val retiredPrisoners = listOf(
      Pair("A8862DW", LocalDate.now().plusDays(1)),
      Pair("A0334EZ", LocalDate.now().plusDays(1)),
      Pair("A1611AF", LocalDate.now().plusDays(1)),
      Pair("A6015FC", LocalDate.now().plusDays(1)),
      Pair("A7345CR", LocalDate.now().plusDays(1)),
      Pair("A4425FC", LocalDate.now().plusDays(1)),
      Pair("A1590FA", LocalDate.now().plusDays(1)),
      Pair("A5091ER", LocalDate.now().plusDays(1)),
      Pair("A5022DQ", LocalDate.now().plusDays(1)),
      Pair("A4812DT", LocalDate.now().plusDays(1)),
      Pair("A1798EF", LocalDate.now().plusDays(1)),
      Pair("A2221CW", LocalDate.now().plusDays(1)),
      Pair("A8764EV", LocalDate.now().plusDays(1)),
      Pair("A2902EY", LocalDate.now().plusDays(1)),
      Pair("A3840EA", LocalDate.now().plusDays(1)),
      Pair("A6655AQ", LocalDate.now().plusDays(1)),
      Pair("A3316CV", LocalDate.now().plusDays(1)),
      Pair("A0593FD", LocalDate.now().plusDays(1)),
      Pair("A4249DC", LocalDate.now().plusDays(1)),
      Pair("A7188AE", LocalDate.now().plusDays(1)),
      Pair("A3161FD", LocalDate.now().plusDays(1)),
      Pair("A4774FD", LocalDate.now().plusDays(1)),
    )
    val retiredPrisonerIdList = retiredPrisoners.map { it.first }

    if (deallocate) {
      jobRunner.runJob(
        JobDefinition(
          JobType.FIX_ZERO_PAY_DEALLOCATE,
        ) { dataFixService.deallocate(retiredActivityIdScheduleId.second, retiredPrisonerIdList, LocalDate.now()) },
      )
    }

    if (makeUnpaid) {
      jobRunner.runJob(
        JobDefinition(
          JobType.FIX_ZERO_PAY_MAKE_UNPAID,
        ) { dataFixService.makeUnpaid("RSI", retiredActivityIdScheduleId.first) },
      )
    }

    if (allocate) {
      jobRunner.runJob(
        JobDefinition(
          JobType.FIX_ZERO_PAY_REALLOCATE,
        ) { dataFixService.allocate(retiredActivityIdScheduleId.second, retiredPrisoners) },
      )
    }

    log.info("Finished running fix paid to unpaid job with deallocate: $deallocate makeUnpaid: $makeUnpaid allocate: $allocate")
  }
}
