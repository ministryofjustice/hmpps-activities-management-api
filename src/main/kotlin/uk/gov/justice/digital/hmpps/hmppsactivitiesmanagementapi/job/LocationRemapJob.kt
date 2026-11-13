package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationRemapResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationRemapService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import javax.sql.DataSource

@Component
class LocationRemapJob(
  private val service: LocationRemapService,
  private val jobRunner: SafeJobRunner,
  private val dataSource: DataSource,
  private val telemetryClient: TelemetryClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Async("asyncExecutor")
  fun execute(file: String, prisons: Set<String>, dryRun: Boolean) {
    jobRunner.runJob(
      JobDefinition(JobType.REMAP_NON_RESIDENTIAL_LOCATIONS) {
        val summary = LocationRemapResult()
        val started = System.nanoTime()
        val properties = mutableMapOf(
          "runId" to summary.runId,
          "file" to file,
          "prisons" to prisons.sorted().joinToString(",").ifEmpty { "ALL" },
          "dryRun" to dryRun.toString(),
          "status" to "FAILED",
        )
        log.info("Starting location remap {}", properties)
        try {
          dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
              statement.executeQuery("select pg_try_advisory_lock(4624, 924)").use { result ->
                check(result.next() && result.getBoolean(1)) { "A location remap job is already running" }
              }
              try {
                service.remap(file, prisons, dryRun, summary)
              } finally {
                statement.execute("select pg_advisory_unlock(4624, 924)")
              }
            }
          }
          properties["status"] = if (dryRun) "DRY_RUN" else "SUCCEEDED"
        } catch (exception: Exception) {
          properties["errorType"] = exception.javaClass.simpleName
          throw exception
        } finally {
          properties["stage"] = summary.stage
          val metrics = summary.metrics() + ("durationMs" to (System.nanoTime() - started) / 1_000_000.0)
          log.info("Location remap result properties={} metrics={}", properties, metrics)
          runCatching { telemetryClient.trackEvent(TelemetryEvent.LOCATION_REMAP_JOB_RESULT.value, properties, metrics) }
            .onFailure { log.error("Failed to publish location remap result telemetry for run ${properties["runId"]}", it) }
        }
      },
    )
  }
}
