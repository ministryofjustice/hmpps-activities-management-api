package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.ServiceUsingLocationDto.ServiceType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSetRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentSetService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

@Service
class LocationRemapService(
  private val files: LocationRemapFile,
  private val schedules: ActivityScheduleRepository,
  private val series: AppointmentSeriesRepository,
  private val sets: AppointmentSetRepository,
  private val activityService: ActivityService,
  private val appointmentService: AppointmentUpdateDomainService,
  private val setService: AppointmentSetService,
  private val locations: LocationService,
  private val features: FeatureSwitches,
  private val clock: Clock,
  private val telemetryClient: TelemetryClient,
  @Value("\${location-remap.allowed-file:}") private val allowedFile: String,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val ADMIN_USERNAME = "activities-management-admin-1"
    val PILOT_PRISONS = setOf("GMI", "EEI", "CLI")
  }

  fun remap(file: String, prisons: Set<String>, dryRun: Boolean, result: LocationRemapResult = LocationRemapResult()) {
    result.stage = "VALIDATION"
    require(prisons.isEmpty() || prisons.all { it.matches(Regex("[A-Z]{3}")) }) { "Invalid prison scope" }
    if (!dryRun) {
      require(file == allowedFile && allowedFile.isNotBlank()) { "CSV is not approved by location-remap.allowed-file for this environment" }
      check(features.isEnabled(Feature.OUTBOUND_EVENTS_ENABLED)) { "Outbound SNS events must be enabled" }
      check(features.isEnabled(OutboundEvent.ACTIVITY_SCHEDULE_UPDATED)) { "Schedule update events must be enabled" }
      check(features.isEnabled(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED)) { "Appointment update events must be enabled" }
    }
    val today = LocalDate.now(clock)
    val mappings = files.load(file).filter { prisons.isEmpty() || it.prisonId in prisons }
    require(mappings.isNotEmpty()) { "No mappings for the selected prison scope" }
    result.totalMappings = mappings.size
    result.stage = "PLANNING"
    val plans = mappings.map { candidates(it, today) }
    result.totalCandidates = plans.sumOf { it.activities.size + it.series.size + it.sets.size }
    log.info("Location remap file={} prisons={} dryRun={} date={} mappings={}", file, prisons.ifEmpty { setOf("ALL") }, dryRun, today, plans.size)
    result.stage = "PREFLIGHT"
    val targets = plans.filter { it.hasCandidates() }.map { it.mapping.chosenChildId }.distinct().associateWith { targetId ->
      runCatching { locations.getDpsLocationDetails(targetId).also { locations.getLocationDetails(targetId) } }
    }
    val preflightFailures = plans.filter { it.hasCandidates() }.mapNotNull { plan ->
      runCatching {
        val target = targets.getValue(plan.mapping.chosenChildId).getOrThrow()
        require(target.prisonId == plan.mapping.prisonId && target.status == Location.Status.ACTIVE && !target.isResidential) { "Destination must be active, non-residential and in the same prison" }
        val services = target.servicesUsingLocation.orEmpty().map { it.serviceType }.toSet()
        require(plan.activities.isEmpty() || ServiceType.PROGRAMMES_AND_ACTIVITIES in services) { "Destination is not enabled for Activities" }
        require((plan.series.isEmpty() && plan.sets.isEmpty()) || ServiceType.APPOINTMENT in services) { "Destination is not enabled for Appointments" }
      }.exceptionOrNull()?.also { failure ->
        log.error("Location remap preflight failed for {}", plan.mapping, failure)
        val target = targets.getValue(plan.mapping.chosenChildId).getOrNull()
        val properties = mapOf(
          "runId" to result.runId,
          "file" to file,
          "prisons" to prisons.sorted().joinToString(",").ifEmpty { "ALL" },
          "dryRun" to dryRun.toString(),
          "prisonId" to plan.mapping.prisonId,
          "sourceDpsLocationId" to plan.mapping.locationId.toString(),
          "destinationDpsLocationId" to plan.mapping.chosenChildId.toString(),
          "sourceLocationName" to plan.mapping.locationName.orEmpty(),
          "destinationLocationName" to (target?.localName ?: target?.code).orEmpty(),
          "errorType" to failure.javaClass.simpleName,
          "reason" to (failure.message ?: failure.javaClass.simpleName),
        )
        val metrics = mapOf(
          "activityCount" to plan.activities.size.toDouble(),
          "appointmentSeriesCount" to plan.series.size.toDouble(),
          "appointmentSetCount" to plan.sets.size.toDouble(),
        )
        runCatching { telemetryClient.trackEvent(TelemetryEvent.LOCATION_REMAP_PREFLIGHT_FAILED.value, properties, metrics) }
          .onFailure { log.error("Failed to publish preflight failure telemetry for run ${result.runId} mapping=${plan.mapping}", it) }
      }
    }
    plans.forEach { log.info("Location remap plan {} activities={} series={} sets={}", it.mapping, it.activities, it.series, it.sets) }
    result.preflightFailures = preflightFailures.size
    check(preflightFailures.isEmpty()) { "Location remap preflight failed for ${preflightFailures.size} mappings; no changes made" }
    if (dryRun) {
      result.stage = "COMPLETED"
      return
    }

    result.stage = "UPDATING"
    plans.forEach { plan ->
      fun update(type: String, id: Long, action: () -> Int) {
        runCatching(action)
          .onSuccess {
            result.totalSucceeded++
            result.totalChanged += it
          }
          .onFailure {
            result.totalFailed++
            log.error("Location remap failed type=$type id=$id mapping=${plan.mapping}; inspect event telemetry before retrying", it)
          }
      }
      plan.activities.forEach { id -> update("activity", id) { activityService.remapLocation(id, plan.mapping, today, ADMIN_USERNAME) } }
      plan.series.forEach { id -> update("series", id) { appointmentService.remapLocation(id, plan.mapping, today, ADMIN_USERNAME) } }
      plan.sets.forEach { id -> update("set", id) { if (setService.remapLocation(id, plan.mapping, today, ADMIN_USERNAME)) 1 else 0 } }
    }
    result.stage = "RESIDUAL_CHECK"
    val remaining = mappings.map { candidates(it, today) }.filter { it.hasCandidates() }
    result.residualMappings = remaining.size
    remaining.forEach { log.error("Location remap residual {} activities={} series={} sets={}", it.mapping, it.activities, it.series, it.sets) }
    log.info("Location remap finished file={} changed={} failures={} residualMappings={}", file, result.totalChanged, result.totalFailed, remaining.size)
    check(result.totalFailed == 0 && remaining.isEmpty()) { "Location remap incomplete: ${result.totalFailed} failures, ${remaining.size} mappings with remaining references" }
    result.stage = "COMPLETED"
  }

  private fun candidates(mapping: LocationRemap, today: LocalDate) = Plan(
    mapping,
    schedules.findActivityIdsForLocationRemap(mapping.prisonId, mapping.locationId, today),
    series.findSeriesIdsForLocationRemap(mapping.prisonId, mapping.locationId, today),
    sets.findSetIdsForLocationRemap(mapping.prisonId, mapping.locationId, today),
  )

  private data class Plan(val mapping: LocationRemap, val activities: List<Long>, val series: List<Long>, val sets: List<Long>) {
    fun hasCandidates() = activities.isNotEmpty() || series.isNotEmpty() || sets.isNotEmpty()
  }
}

class LocationRemapResult {
  val runId: String = UUID.randomUUID().toString()
  var stage: String = "LOCK"
  var totalMappings: Int = 0
  var totalCandidates: Int = 0
  var totalSucceeded: Int = 0
  var totalFailed: Int = 0
  var totalChanged: Int = 0
  var preflightFailures: Int = 0
  var residualMappings: Int = 0

  fun metrics() = mapOf(
    "totalMappings" to totalMappings.toDouble(),
    "totalCandidates" to totalCandidates.toDouble(),
    "totalSucceeded" to totalSucceeded.toDouble(),
    "totalFailed" to totalFailed.toDouble(),
    "totalChanged" to totalChanged.toDouble(),
    "preflightFailures" to preflightFailures.toDouble(),
    "residualMappings" to residualMappings.toDouble(),
  )
}
