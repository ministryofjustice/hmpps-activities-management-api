package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
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
  private val today = LocalDate.of(2026, 10, 19)
  private val clock = Clock.fixed(today.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
  private val mapping = LocationRemap("GMI", UUID.randomUUID(), UUID.randomUUID())
  private val service = LocationRemapService(files, schedules, series, sets, activityService, appointmentService, setService, locations, features, clock, "mapping.csv")

  @BeforeEach
  fun setUp() {
    whenever(files.load("mapping.csv")).thenReturn(listOf(mapping, mapping.copy(prisonId = "MDI", locationId = UUID.randomUUID())))
    whenever(features.isEnabled(Feature.OUTBOUND_EVENTS_ENABLED)).thenReturn(true)
    whenever(features.isEnabled(OutboundEvent.ACTIVITY_SCHEDULE_UPDATED)).thenReturn(true)
    whenever(features.isEnabled(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED)).thenReturn(true)
    whenever(locations.getDpsLocationDetails(mapping.chosenChildId)).thenReturn(
      dpsLocation(mapping.chosenChildId, "GMI", "CHILD", "Laundry").copy(
        servicesUsingLocation = listOf(
          ServiceUsingLocationDto(ServiceUsingLocationDto.ServiceType.PROGRAMMES_AND_ACTIVITIES, ServiceUsingLocationDto.ServiceFamilyType.ACTIVITIES_APPOINTMENTS),
          ServiceUsingLocationDto(ServiceUsingLocationDto.ServiceType.APPOINTMENT, ServiceUsingLocationDto.ServiceFamilyType.ACTIVITIES_APPOINTMENTS),
        ),
      ),
    )
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
}
