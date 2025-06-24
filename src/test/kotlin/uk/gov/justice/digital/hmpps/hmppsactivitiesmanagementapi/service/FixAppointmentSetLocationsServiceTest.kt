package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api.LocationsInsidePrisonAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisMappingAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSetRepository
import java.util.*

class FixAppointmentSetLocationsServiceTest {
  private val appointmentSetRepository: AppointmentSetRepository = mock()
  private val nomisMappingAPIClient: NomisMappingAPIClient = mock()
  private val locationsInsidePrisonAPIClient: LocationsInsidePrisonAPIClient = mock()

  private val uuid1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
  private val uuid2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
  private val uuid3 = UUID.fromString("33333333-3333-3333-3333-333333333333")

  private val fixAppointmentSetLocationsService = FixAppointmentSetLocationsService(
    appointmentSetRepository,
    nomisMappingAPIClient,
    locationsInsidePrisonAPIClient,
    TransactionHandler(),
  )

  @BeforeEach
  fun setUp() {
    whenever(nomisMappingAPIClient.getLocationMappingByNomisId(1)).thenReturn(NomisDpsLocationMapping(uuid1, 1))
    whenever(nomisMappingAPIClient.getLocationMappingByNomisId(2)).thenReturn(NomisDpsLocationMapping(uuid2, 2))
    whenever(nomisMappingAPIClient.getLocationMappingByNomisId(3)).thenReturn(NomisDpsLocationMapping(uuid3, 3))

    whenever(locationsInsidePrisonAPIClient.getLocationById(uuid1)).thenReturn(dpsLocation(uuid1, "RSI", "code1", "localName1"))
    whenever(locationsInsidePrisonAPIClient.getLocationById(uuid2)).thenReturn(dpsLocation(uuid2, "RSI", "code2", "localName2"))
    whenever(locationsInsidePrisonAPIClient.getLocationById(uuid3)).thenReturn(dpsLocation(uuid3, "RSI", "code3", "localName3"))
  }

  @Test
  fun `should update location details`() {
    whenever(appointmentSetRepository.findNomisLocationsIds()).thenReturn(listOf(1, 2, 3))

    fixAppointmentSetLocationsService.fixLocations()

    verify(appointmentSetRepository).findNomisLocationsIds()
    verify(appointmentSetRepository).updateLocationDetails(1, uuid1)
    verify(appointmentSetRepository).updateLocationDetails(2, uuid2)
    verify(appointmentSetRepository).updateLocationDetails(3, uuid3)
    verifyNoMoreInteractions(appointmentSetRepository)
  }

  @Test
  fun `should continue if mapping API client threw an exception`() {
    whenever(appointmentSetRepository.findNomisLocationsIds()).thenReturn(listOf(1, 2, 3))

    whenever(nomisMappingAPIClient.getLocationMappingByNomisId(2)).thenThrow(RuntimeException())

    fixAppointmentSetLocationsService.fixLocations()

    verify(appointmentSetRepository).findNomisLocationsIds()
    verify(appointmentSetRepository).updateLocationDetails(1, uuid1)
    verify(appointmentSetRepository).updateLocationDetails(3, uuid3)
    verifyNoMoreInteractions(appointmentSetRepository)
  }

  @Test
  fun `should continue if locations inside prison API client threw an exception`() {
    whenever(appointmentSetRepository.findNomisLocationsIds()).thenReturn(listOf(1, 2, 3))

    whenever(locationsInsidePrisonAPIClient.getLocationById(uuid2)).thenThrow(RuntimeException())

    fixAppointmentSetLocationsService.fixLocations()

    verify(appointmentSetRepository).findNomisLocationsIds()
    verify(appointmentSetRepository).updateLocationDetails(1, uuid1)
    verify(appointmentSetRepository).updateLocationDetails(3, uuid3)
    verifyNoMoreInteractions(appointmentSetRepository)
  }
}
