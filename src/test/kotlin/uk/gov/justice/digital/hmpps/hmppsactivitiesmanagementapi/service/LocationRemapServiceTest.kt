package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.ServiceUsingLocationDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSetRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentSetService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class LocationRemapServiceTest {
  private val files: LocationRemapFile = mock()
  private val schedules: ActivityScheduleRepository = mock()
  private val series: AppointmentSeriesRepository = mock()
  private val sets: AppointmentSetRepository = mock()
  private val activityService: ActivityService = mock()
  private val appointmentService: AppointmentUpdateDomainService = mock()
  private val setService: AppointmentSetService = mock()
  private val locations: LocationService = mock()
  private val features: FeatureSwitches = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val today = LocalDate.of(2026, 10, 19)
  private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
  private val mapping = LocationRemap("GMI", UUID.randomUUID(), UUID.randomUUID())
  private val service = LocationRemapService(files, schedules, series, sets, activityService, appointmentService, setService, locations, features, clock, telemetryClient, "mapping.csv")

  @BeforeEach
  fun setUp() {
    whenever(files.load("mapping.csv")).thenReturn(listOf(mapping, mapping.copy(prisonId = "MDI", locationId = UUID.randomUUID())))
    whenever(features.isEnabled(Feature.OUTBOUND_EVENTS_ENABLED)).thenReturn(true)
    whenever(features.isEnabled(OutboundEvent.ACTIVITY_SCHEDULE_UPDATED)).thenReturn(true)
    whenever(features.isEnabled(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED)).thenReturn(true)
    whenever(locations.getDpsLocationDetails(mapping.chosenChildId)).thenReturn(destination(mapping.chosenChildId))
    whenever(locations.getLocationDetails(mapping.chosenChildId)).thenReturn(LocationService.LocationDetails("GMI", 123, mapping.chosenChildId, "CHILD", "Laundry"))
  }

  @Test
  fun `dry run preflights candidates but never writes`() {
    whenever(schedules.findActivityIdsForLocationRemap("GMI", mapping.locationId, today)).thenReturn(listOf(1))
    val result = LocationRemapResult()
    service.remap("mapping.csv", setOf("GMI"), true, result)
    assertThat(result.totalMappings).isEqualTo(1)
    assertThat(result.totalCandidates).isEqualTo(1)
    assertThat(result.totalSucceeded).isZero()
    assertThat(result.totalFailed).isZero()
    assertThat(result.stage).isEqualTo("COMPLETED")
    verify(locations).getDpsLocationDetails(mapping.chosenChildId)
    verifyNoInteractions(activityService, appointmentService, setService)
    verifyNoInteractions(telemetryClient)
  }

  @Test
  fun `live run updates every object type and verifies residuals`() {
    whenever(schedules.findActivityIdsForLocationRemap("GMI", mapping.locationId, today)).thenReturn(listOf(1), emptyList())
    whenever(series.findSeriesIdsForLocationRemap("GMI", mapping.locationId, today)).thenReturn(listOf(2), emptyList())
    whenever(sets.findSetIdsForLocationRemap("GMI", mapping.locationId, today)).thenReturn(listOf(3), emptyList())
    whenever(activityService.remapLocation(1, mapping, today, "activities-management-admin-1")).thenReturn(2)
    whenever(appointmentService.remapLocation(2, mapping, today, "activities-management-admin-1")).thenReturn(4)
    whenever(setService.remapLocation(3, mapping, today, "activities-management-admin-1")).thenReturn(true)
    val result = LocationRemapResult()
    service.remap("mapping.csv", setOf("GMI"), false, result)
    assertThat(result.totalCandidates).isEqualTo(3)
    assertThat(result.totalSucceeded).isEqualTo(3)
    assertThat(result.totalFailed).isZero()
    assertThat(result.totalChanged).isEqualTo(7)
    assertThat(result.residualMappings).isZero()
    verify(activityService).remapLocation(1, mapping, today, "activities-management-admin-1")
    verify(appointmentService).remapLocation(2, mapping, today, "activities-management-admin-1")
    verify(setService).remapLocation(3, mapping, today, "activities-management-admin-1")
  }

  @Test
  fun `missing target fails before any writes`() {
    whenever(schedules.findActivityIdsForLocationRemap("GMI", mapping.locationId, today)).thenReturn(listOf(1))
    whenever(locations.getDpsLocationDetails(mapping.chosenChildId)).thenThrow(IllegalArgumentException("Missing target"))
    val result = LocationRemapResult()
    assertThatThrownBy { service.remap("mapping.csv", setOf("GMI"), false, result) }.hasMessageContaining("preflight")
    assertThat(result.preflightFailures).isEqualTo(1)
    assertThat(result.totalSucceeded).isZero()
    assertThat(result.totalFailed).isZero()
    assertThat(result.stage).isEqualTo("PREFLIGHT")
    verifyNoInteractions(activityService, appointmentService, setService)
    val properties = argumentCaptor<Map<String, String>>()
    val metrics = argumentCaptor<Map<String, Double>>()
    verify(telemetryClient).trackEvent(eq(TelemetryEvent.LOCATION_REMAP_PREFLIGHT_FAILED.value), properties.capture(), metrics.capture())
    assertThat(properties.firstValue).containsEntry("runId", result.runId)
      .containsEntry("file", "mapping.csv")
      .containsEntry("prisonId", "GMI")
      .containsEntry("sourceDpsLocationId", mapping.locationId.toString())
      .containsEntry("destinationDpsLocationId", mapping.chosenChildId.toString())
      .containsEntry("errorType", "IllegalArgumentException")
      .containsEntry("reason", "Missing target")
      .containsEntry("dryRun", "false")
    assertThat(metrics.firstValue).containsEntry("activityCount", 1.0)
  }

  @ParameterizedTest
  @ValueSource(strings = ["INACTIVE", "RESIDENTIAL", "WRONG_PRISON", "ACTIVITIES_NOT_ENABLED", "APPOINTMENTS_NOT_ENABLED"])
  fun `invalid destination prevents updates even when another mapping is valid`(invalidReason: String) {
    val validMapping = mapping.copy(locationId = UUID.randomUUID(), chosenChildId = UUID.randomUUID())
    whenever(files.load("mapping.csv")).thenReturn(listOf(validMapping, mapping))
    listOf(validMapping, mapping).forEach { candidate ->
      whenever(schedules.findActivityIdsForLocationRemap("GMI", candidate.locationId, today)).thenReturn(listOf(1))
      whenever(series.findSeriesIdsForLocationRemap("GMI", candidate.locationId, today)).thenReturn(listOf(2))
      whenever(sets.findSetIdsForLocationRemap("GMI", candidate.locationId, today)).thenReturn(listOf(3))
    }
    val validDestination = destination(mapping.chosenChildId)
    val invalidDestination = when (invalidReason) {
      "INACTIVE" -> validDestination.copy(status = Location.Status.INACTIVE)
      "RESIDENTIAL" -> validDestination.copy(isResidential = true)
      "WRONG_PRISON" -> validDestination.copy(prisonId = "MDI")
      "ACTIVITIES_NOT_ENABLED" -> validDestination.copy(servicesUsingLocation = validDestination.servicesUsingLocation!!.filterNot { it.serviceType == ServiceUsingLocationDto.ServiceType.PROGRAMMES_AND_ACTIVITIES })
      "APPOINTMENTS_NOT_ENABLED" -> validDestination.copy(servicesUsingLocation = validDestination.servicesUsingLocation!!.filterNot { it.serviceType == ServiceUsingLocationDto.ServiceType.APPOINTMENT })
      else -> error("Unknown invalid destination")
    }
    whenever(locations.getDpsLocationDetails(mapping.chosenChildId)).thenReturn(invalidDestination)
    whenever(locations.getDpsLocationDetails(validMapping.chosenChildId)).thenReturn(destination(validMapping.chosenChildId))
    whenever(locations.getLocationDetails(validMapping.chosenChildId)).thenReturn(LocationService.LocationDetails("GMI", 456, validMapping.chosenChildId, "CHILD", "Laundry"))
    val result = LocationRemapResult()

    assertThatThrownBy { service.remap("mapping.csv", setOf("GMI"), false, result) }.hasMessageContaining("preflight")

    assertThat(result.totalMappings).isEqualTo(2)
    assertThat(result.preflightFailures).isEqualTo(1)
    assertThat(result.stage).isEqualTo("PREFLIGHT")
    verifyNoInteractions(activityService, appointmentService, setService)
    val properties = argumentCaptor<Map<String, String>>()
    val metrics = argumentCaptor<Map<String, Double>>()
    verify(telemetryClient).trackEvent(eq(TelemetryEvent.LOCATION_REMAP_PREFLIGHT_FAILED.value), properties.capture(), metrics.capture())
    assertThat(properties.firstValue).containsEntry("runId", result.runId)
      .containsEntry("sourceDpsLocationId", mapping.locationId.toString())
      .containsEntry("destinationDpsLocationId", mapping.chosenChildId.toString())
      .containsEntry("destinationLocationName", "Laundry")
    assertThat(properties.firstValue["reason"]).isNotBlank()
    assertThat(metrics.firstValue).containsEntry("activityCount", 1.0)
      .containsEntry("appointmentSeriesCount", 1.0)
      .containsEntry("appointmentSetCount", 1.0)
  }

  @ParameterizedTest(name = "telemetry unavailable: {0}")
  @ValueSource(booleans = [false, true])
  fun `reports every failed mapping during dry run even when telemetry throws`(telemetryUnavailable: Boolean) {
    val firstMapping = mapping.copy(locationName = "First old room")
    val secondMapping = mapping.copy(locationId = UUID.randomUUID(), chosenChildId = UUID.randomUUID(), locationName = "Second old room")
    whenever(files.load("mapping.csv")).thenReturn(listOf(firstMapping, secondMapping))
    listOf(firstMapping, secondMapping).forEach { candidate ->
      whenever(schedules.findActivityIdsForLocationRemap("GMI", candidate.locationId, today)).thenReturn(listOf(1))
    }
    whenever(locations.getDpsLocationDetails(firstMapping.chosenChildId)).thenReturn(destination(firstMapping.chosenChildId).copy(servicesUsingLocation = emptyList()))
    whenever(locations.getDpsLocationDetails(secondMapping.chosenChildId)).thenThrow(IllegalArgumentException("Second destination missing"))
    if (telemetryUnavailable) {
      whenever(telemetryClient.trackEvent(eq(TelemetryEvent.LOCATION_REMAP_PREFLIGHT_FAILED.value), any(), any())).thenThrow(IllegalStateException("Telemetry unavailable"))
    }
    val result = LocationRemapResult()

    assertThatThrownBy { service.remap("mapping.csv", setOf("GMI"), true, result) }.hasMessageContaining("preflight failed for 2 mappings")

    assertThat(result.preflightFailures).isEqualTo(2)
    val properties = argumentCaptor<Map<String, String>>()
    verify(telemetryClient, times(2)).trackEvent(eq(TelemetryEvent.LOCATION_REMAP_PREFLIGHT_FAILED.value), properties.capture(), any())
    assertThat(properties.allValues.map { it["sourceDpsLocationId"] }).containsExactly(firstMapping.locationId.toString(), secondMapping.locationId.toString())
    assertThat(properties.allValues.map { it["sourceLocationName"] }).containsExactly("First old room", "Second old room")
    assertThat(properties.allValues.map { it["reason"] }).containsExactly("Destination is not enabled for Activities", "Second destination missing")
    properties.allValues.forEach {
      assertThat(it).containsEntry("runId", result.runId)
        .containsEntry("prisons", "GMI")
        .containsEntry("dryRun", "true")
    }
    verifyNoInteractions(activityService, appointmentService, setService)
  }

  @Test
  fun `partial failures are reported after continuing other objects`() {
    whenever(schedules.findActivityIdsForLocationRemap("GMI", mapping.locationId, today)).thenReturn(listOf(1))
    whenever(series.findSeriesIdsForLocationRemap("GMI", mapping.locationId, today)).thenReturn(listOf(2))
    whenever(activityService.remapLocation(eq(1), any(), any(), any())).thenThrow(IllegalStateException("Publish failed"))
    val result = LocationRemapResult()
    assertThatThrownBy { service.remap("mapping.csv", setOf("GMI"), false, result) }.hasMessageContaining("incomplete")
    assertThat(result.totalSucceeded).isEqualTo(1)
    assertThat(result.totalFailed).isEqualTo(1)
    assertThat(result.residualMappings).isEqualTo(1)
    verify(appointmentService).remapLocation(2, mapping, today, "activities-management-admin-1")
  }

  @Test
  fun `unapproved file and disabled SNS prevent live runs`() {
    assertThatThrownBy { service.remap("other.csv", emptySet(), false) }.hasMessageContaining("approved")
    whenever(features.isEnabled(Feature.OUTBOUND_EVENTS_ENABLED)).thenReturn(false)
    assertThatThrownBy { service.remap("mapping.csv", emptySet(), false) }.hasMessageContaining("SNS")
    verifyNoInteractions(activityService, appointmentService, setService)
  }

  @Test
  fun `already migrated mappings are a no op`() {
    val result = LocationRemapResult()
    service.remap("mapping.csv", setOf("GMI"), false, result)
    assertThat(result.totalSucceeded).isZero()
    assertThat(result.totalFailed).isZero()
    assertThat(result.totalCandidates).isZero()
    verifyNoInteractions(activityService, appointmentService, setService, locations)
  }

  private fun destination(id: UUID) = dpsLocation(id, "GMI", "CHILD", "Laundry").copy(
    servicesUsingLocation = listOf(
      ServiceUsingLocationDto(ServiceUsingLocationDto.ServiceType.PROGRAMMES_AND_ACTIVITIES, ServiceUsingLocationDto.ServiceFamilyType.ACTIVITIES_APPOINTMENTS),
      ServiceUsingLocationDto(ServiceUsingLocationDto.ServiceType.APPOINTMENT, ServiceUsingLocationDto.ServiceFamilyType.ACTIVITIES_APPOINTMENTS),
    ),
  )
}
