package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationRemapResult
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationRemapService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.DataSource

class LocationRemapJobTest : JobsTestBase() {
  private val service: LocationRemapService = mock()
  private val dataSource: DataSource = mock()
  private val connection: Connection = mock()
  private val statement: Statement = mock()
  private val result: ResultSet = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val properties = argumentCaptor<Map<String, String>>()
  private val metrics = argumentCaptor<Map<String, Double>>()
  private val job = LocationRemapJob(service, safeJobRunner, dataSource, telemetryClient)

  @BeforeEach
  fun setUp() {
    mockJobs(JobType.REMAP_NON_RESIDENTIAL_LOCATIONS)
    whenever(dataSource.connection).thenReturn(connection)
    whenever(connection.createStatement()).thenReturn(statement)
    whenever(statement.executeQuery("select pg_try_advisory_lock(4624, 924)")).thenReturn(result)
    whenever(result.next()).thenReturn(true)
    whenever(result.getBoolean(1)).thenReturn(true)
  }

  @Test
  fun `runs through safe job runner and releases the database lock`() {
    job.execute("mapping.csv", setOf("GMI"), true)
    verify(safeJobRunner).runJob(argThat { jobType == JobType.REMAP_NON_RESIDENTIAL_LOCATIONS })
    verify(service).remap(eq("mapping.csv"), eq(setOf("GMI")), eq(true), any())
    verify(statement).execute("select pg_advisory_unlock(4624, 924)")
    verify(connection).close()
    verifySummary("DRY_RUN", 0.0, 0.0)
  }

  @Test
  fun `does not run when another replica holds the lock`() {
    whenever(result.getBoolean(1)).thenReturn(false)
    job.execute("mapping.csv", emptySet(), false)
    verifyNoInteractions(service)
    verify(connection).close()
    verifySummary("FAILED", 0.0, 0.0)
    assertThat(properties.firstValue["stage"]).isEqualTo("LOCK")
  }

  @Test
  fun `releases lock after migration failure`() {
    whenever(service.remap(eq("mapping.csv"), eq(emptySet()), eq(false), any())).thenThrow(IllegalStateException("Failed preflight"))
    job.execute("mapping.csv", emptySet(), false)
    verify(statement).execute("select pg_advisory_unlock(4624, 924)")
    verify(connection).close()
    verifySummary("FAILED", 0.0, 0.0)
  }

  @Test
  fun `publishes successful update totals`() {
    whenever(service.remap(eq("mapping.csv"), eq(emptySet()), eq(false), any())).thenAnswer {
      it.getArgument<LocationRemapResult>(3).apply {
        totalSucceeded = 3
        totalChanged = 8
        stage = "COMPLETED"
      }
      Unit
    }
    job.execute("mapping.csv", emptySet(), false)
    verifySummary("SUCCEEDED", 3.0, 0.0)
    assertThat(metrics.firstValue["totalChanged"]).isEqualTo(8.0)
  }

  @Test
  fun `publishes partial totals when migration fails`() {
    whenever(service.remap(eq("mapping.csv"), eq(emptySet()), eq(false), any())).thenAnswer {
      it.getArgument<LocationRemapResult>(3).apply {
        totalSucceeded = 2
        totalFailed = 1
        stage = "RESIDUAL_CHECK"
      }
      throw IllegalStateException("Incomplete")
    }
    job.execute("mapping.csv", emptySet(), false)
    verifySummary("FAILED", 2.0, 1.0)
    assertThat(properties.firstValue["errorType"]).isEqualTo("IllegalStateException")
  }

  @Test
  fun `telemetry client failure does not change migration outcome or prevent lock release`() {
    whenever(telemetryClient.trackEvent(eq(TelemetryEvent.LOCATION_REMAP_JOB_RESULT.value), any(), any())).thenThrow(IllegalStateException("Telemetry unavailable"))
    job.execute("mapping.csv", emptySet(), false)
    verifySummary("SUCCEEDED", 0.0, 0.0)
    verify(statement).execute("select pg_advisory_unlock(4624, 924)")
    verify(connection).close()
  }

  private fun verifySummary(status: String, succeeded: Double, failed: Double) {
    verify(telemetryClient).trackEvent(eq(TelemetryEvent.LOCATION_REMAP_JOB_RESULT.value), properties.capture(), metrics.capture())
    assertThat(properties.firstValue["status"]).isEqualTo(status)
    assertThat(properties.firstValue["file"]).isEqualTo("mapping.csv")
    assertThat(properties.firstValue["prisons"]).isEqualTo(if (status == "DRY_RUN") "GMI" else "ALL")
    assertThat(properties.firstValue["dryRun"]).isEqualTo((status == "DRY_RUN").toString())
    assertThat(properties.firstValue["runId"]).isNotBlank()
    assertThat(metrics.firstValue["totalSucceeded"]).isEqualTo(succeeded)
    assertThat(metrics.firstValue["totalFailed"]).isEqualTo(failed)
    assertThat(metrics.firstValue["durationMs"]).isGreaterThanOrEqualTo(0.0)
  }
}
