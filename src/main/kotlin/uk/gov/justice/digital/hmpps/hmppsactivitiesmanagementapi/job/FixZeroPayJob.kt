package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.DataFixService

@Component
class FixZeroPayJob(
  private val dataFixService: DataFixService,
  private val jobRunner: SafeJobRunner,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute(deallocate: Boolean = false, makeUnpaid: Boolean = false, allocate: Boolean = false, prisonCode: String, activityScheduleId: Long) {
    log.info("Running data fix job for activityScheduleId: $activityScheduleId prisonCode: $prisonCode with deallocate: $deallocate makeUnpaid: $makeUnpaid allocate: $allocate")

    if (activityScheduleId == -1L || prisonCode == "xxx") {
      log.info("Finished running data fix job: Nothing to do for activityScheduleId: $activityScheduleId prisonCode: $prisonCode")
    } else {
      if (deallocate) {
        jobRunner.runJob(
          JobDefinition(
            JobType.FIX_ZERO_PAY_DEALLOCATE,
          ) {
            dataFixService.deallocate(activityScheduleId)
          },
        )
      }

      if (makeUnpaid) {
        jobRunner.runJob(
          JobDefinition(
            JobType.FIX_ZERO_PAY_MAKE_UNPAID,
          ) { dataFixService.makeUnpaid(prisonCode = prisonCode, activityScheduleId = activityScheduleId) },
        )
      }

      if (allocate) {
        jobRunner.runJob(
          JobDefinition(
            JobType.FIX_ZERO_PAY_REALLOCATE,
          ) { dataFixService.reallocate(activityScheduleId) },
        )
      }
      log.info("Finished running data fix job for activityScheduleId: $activityScheduleId prisonCode: $prisonCode with deallocate: $deallocate makeUnpaid: $makeUnpaid allocate: $allocate")
    }
  }
}
