package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.DailyAppointmentMetricsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.RolloutPrisonService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ScheduleReasonEventType
import java.time.LocalDate
import kotlin.system.measureTimeMillis

@Component
class AppointmentMetricsJob(
  private val jobRunner: SafeJobRunner,
  private val rolloutPrisonService: RolloutPrisonService,
  private val prisonApiClient: PrisonApiApplicationClient,
  private val service: DailyAppointmentMetricsService,

) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute() {
    jobRunner.runJob(
      JobDefinition(JobType.APPOINTMENTS_METRICS) {
        log.info("Generating daily appointments metrics")

        val elapsed = measureTimeMillis {
          val allPrisonCodes = rolloutPrisonService.getRolloutPrisons().map { it.prisonCode }
          val allAppointmentCategories = prisonApiClient.getScheduleReasons(ScheduleReasonEventType.APPOINTMENT.value).map { it.code }
          val yesterday = LocalDate.now().minusDays(1)

          allPrisonCodes.forEach { prisonCode ->
            allAppointmentCategories.forEach { categoryCode ->
              service.generateAppointmentMetrics(prisonCode, categoryCode, yesterday)
            }
          }
        }

        log.info("Generating daily appointments metrics took ${elapsed}ms")
      },
    )
  }
}
