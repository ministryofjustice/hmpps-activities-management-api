package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.AdjudicationsHearingAdapter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.api.LocationsInsidePrisonAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisMappingAPIClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.LocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalTimeRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityFromDbInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentInstanceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.internalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEventsSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.EventPriorities
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.Priority
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.ReferenceCodeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class InternalLocationServiceTest {
  private val appointmentAttendeeSearchRepository: AppointmentAttendeeSearchRepository = mock()
  private val appointmentInstanceRepository: AppointmentInstanceRepository = mock()
  private val appointmentSearchRepository: AppointmentSearchRepository = mock()
  private val appointmentSearchSpecification: AppointmentSearchSpecification = spy()
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository = mock()
  private val prisonRegimeService: PrisonRegimeService = mock()
  private val referenceCodeService: ReferenceCodeService = mock()
  private val adjudicationsHearingAdapter: AdjudicationsHearingAdapter = mock()
  private val nomisMappingAPIClient: NomisMappingAPIClient = mock()
  private val locationsInsidePrisonAPIClient: LocationsInsidePrisonAPIClient = mock()

  val service = InternalLocationService(
    appointmentAttendeeSearchRepository,
    appointmentInstanceRepository,
    appointmentSearchRepository,
    appointmentSearchSpecification,
    prisonApiClient,
    prisonerScheduledActivityRepository,
    prisonRegimeService,
    referenceCodeService,
    adjudicationsHearingAdapter,
    nomisMappingAPIClient,
    locationsInsidePrisonAPIClient,
  )

  private val prisonCode: String = "MDI"
  private val date = LocalDate.now()

  private val timeSlotAm = Pair(LocalTime.of(8, 30), LocalTime.of(11, 45))
  private val timeSlotPm = Pair(LocalTime.of(13, 45), LocalTime.of(16, 45))
  private val timeSlotEd = Pair(LocalTime.of(17, 30), LocalTime.of(19, 15))

  private val education1Location = internalLocation(
    locationId = 2L,
    description = "EDUC-ED1-ED1",
    userDescription = "Education 1",
  )
  private val education1DpsLocation = dpsLocation(
    id = UUID.fromString("22222222-2222-2222-2222-222222222222"),
    code = "EDUC-ED1-ED1",
    localName = "Education 1",
  )
  private val education1LocationSummary = LocationSummary(
    locationId = 2L,
    description = "EDUC-ED1-ED1",
    userDescription = "Education 1",
  )
  private val education2Location = internalLocation(
    locationId = 3L,
    description = "EDUC-ED2-ED2",
    userDescription = "Education 2",
  )
  private val education2DpsLocation = dpsLocation(
    id = UUID.fromString("33333333-3333-3333-3333-333333333333"),
    code = "ED2",
    localName = "Education 2",
  )
  private val education2LocationDetails = LocationService.LocationDetails(
    agencyId = prisonCode,
    locationId = education2Location.locationId,
    dpsLocationId = education2DpsLocation.id,
    code = education2DpsLocation.code,
    description = education2DpsLocation.localName!!,
    pathHierarchy = education2DpsLocation.pathHierarchy,
  )
  private val education2LocationSummary = LocationSummary(
    locationId = 3L,
    description = "EDUC-ED2-ED2",
    userDescription = "Education 2",
  )
  private val inactiveEducation1Location = internalLocation(
    locationId = 1L,
    description = "EDUC-ED1",
    userDescription = "Education 1",
  )
  private val noUserDescriptionLocation = internalLocation(
    locationId = 4L,
    description = "NO-USR-DESC",
    userDescription = null,
  )
  private val noUserDescriptionDpsLocation = dpsLocation(
    id = UUID.fromString("44444444-4444-4444-4444-444444444444"),
    code = "NO-USR-DESC",
    localName = null,
  )
  private val socialVisitsLocation = internalLocation(
    locationId = 5L,
    description = "SOCIAL VISITS",
    userDescription = "Social Visits",
  )
  private val socialVisitsDpsLocation = dpsLocation(
    id = UUID.fromString("55555555-5555-5555-5555-555555555555"),
    code = "SOCIAL VISITS",
    localName = "Social Visits",
  )
  private val socialVisitsLocationSummary = LocationSummary(
    locationId = 5L,
    description = "SOCIAL VISITS",
    userDescription = "Social Visits",
  )
  private val education3LocationSummary = LocationSummary(
    locationId = 6L,
    description = "EDUC-ED1-ED3",
    userDescription = "Education 3",
  )

  private val adjudicationLocation = internalLocation(
    locationId = 1000L,
    description = "ADJU",
    userDescription = "ADJU",
  )
  private val adjudicationDpsLocation = dpsLocation(
    id = UUID.fromString("10001000-1000-1000-1000-100010001000"),
    code = "ADJU",
    localName = "ADJU",
  )

  private val education1Appointment = appointmentSearchEntity(
    prisonCode = prisonCode,
    internalLocationId = education1Location.locationId,
    startTime = LocalTime.of(9, 0),
    endTime = LocalTime.of(10, 30),
  )
  private val noUserDescriptionLocationAppointment = appointmentSearchEntity(
    prisonCode = prisonCode,
    internalLocationId = noUserDescriptionLocation.locationId,
    startTime = LocalTime.of(14, 0),
    endTime = LocalTime.of(15, 30),
  )
  private val noLocationAppointment = appointmentSearchEntity(
    prisonCode = prisonCode,
    inCell = true,
    startTime = LocalTime.of(18, 0),
    endTime = LocalTime.of(18, 30),
  )

  private val education1Activity = activityFromDbInstance(
    scheduledInstanceId = 1,
    internalLocationId = education1Location.locationId.toInt(),
    startTime = LocalTime.of(8, 30),
    endTime = LocalTime.of(11, 45),
  )
  private val education2Activity = activityFromDbInstance(
    scheduledInstanceId = 2,
    internalLocationId = education2Location.locationId.toInt(),
    startTime = LocalTime.of(8, 30),
    endTime = LocalTime.of(11, 45),
  )
  private val inactiveEducation1Activity = activityFromDbInstance(
    scheduledInstanceId = 3,
    internalLocationId = inactiveEducation1Location.locationId.toInt(),
    startTime = LocalTime.of(13, 45),
    endTime = LocalTime.of(16, 45),
  )
  private val noLocationActivity = activityFromDbInstance(
    scheduledInstanceId = 4,
    internalLocationId = null,
    startTime = LocalTime.of(17, 30),
    endTime = LocalTime.of(19, 15),
  )
  private val education3DpsLocation = dpsLocation(
    id = UUID.fromString("66666666-6666-6666-6666-666666666666"),
    code = "EDUC-ED1-ED3",
    localName = "Education 3",
  )

  private val education1AppointmentInstance = appointmentInstanceEntity(
    appointmentInstanceId = 5,
    internalLocationId = education1Location.locationId,
  )
  private val education2AppointmentInstance = appointmentInstanceEntity(
    appointmentInstanceId = 6,
    internalLocationId = education2Location.locationId,
  )
  private val noLocationAppointmentInstance = appointmentInstanceEntity(
    appointmentInstanceId = 7,
    inCell = true,
  )

  private val education1Visit = PrisonApiPrisonerScheduleFixture.visitInstance(
    eventId = 8,
    locationId = education1Location.locationId,
    date = LocalDate.now(),
  )
  private val education2Visit = PrisonApiPrisonerScheduleFixture.visitInstance(
    eventId = 9,
    locationId = education2Location.locationId,
    date = LocalDate.now(),
  )
  private val noLocationVisit = PrisonApiPrisonerScheduleFixture.visitInstance(
    eventId = 9,
    locationId = null,
    date = LocalDate.now(),
  )
  private val socialVisit = PrisonApiPrisonerScheduleFixture.visitInstance(
    eventId = 10,
    locationId = socialVisitsLocationSummary.locationId,
    date = LocalDate.now(),
  )

  private val adjudicationHearing = OffenderAdjudicationHearing(
    internalLocationId = education2Location.locationId,
    agencyId = "",
    hearingId = 1,
    offenderNo = "",
    startTime = LocalDateTime.now().toIsoDateTime(),
  )

  private val adjudicationHearingForEvent = OffenderAdjudicationHearing(
    internalLocationId = education1Location.locationId,
    agencyId = "",
    hearingId = 1,
    offenderNo = "",
    startTime = LocalDateTime.now().toIsoDateTime(),
  )

  @BeforeEach
  fun setUp() {
    locationsInsidePrisonAPIClient.stub {
      on {
        runBlocking {
          getNonResidentialLocations(prisonCode)
        }
      } doReturn listOf(
        education1DpsLocation,
        education2DpsLocation,
        noUserDescriptionDpsLocation,
        socialVisitsDpsLocation,
        adjudicationDpsLocation,
        education3DpsLocation,
      )
    }

    prisonRegimeService.stub {
      on {
        getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.AM, LocalDate.now().dayOfWeek)
      } doReturn LocalTimeRange(timeSlotAm.first, timeSlotAm.second)
      on {
        getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.PM, LocalDate.now().dayOfWeek)
      } doReturn LocalTimeRange(timeSlotPm.first, timeSlotPm.second)
      on {
        getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.ED, LocalDate.now().dayOfWeek)
      } doReturn LocalTimeRange(timeSlotEd.first, timeSlotEd.second)
    }

    adjudicationsHearingAdapter.stub {
      on {
        runBlocking {
          getAdjudicationsByLocation(any(), any(), anyOrNull(), any())
        }
      } doReturn mapOf(
        adjudicationLocation.locationId to listOf(adjudicationHearing),
      )
    }
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Nested
  @DisplayName("getInternalLocationsMapByIds")
  inner class GetInternalLocationsMapByIds {
    @Test
    fun `filters locations matching supplied ids`() = runBlocking {
      whenever(locationsInsidePrisonAPIClient.getNonResidentialLocations(prisonCode)).thenReturn(listOf(education2DpsLocation))

      whenever(nomisMappingAPIClient.getLocationMappingsByDpsIds(setOf(education2DpsLocation.id))).thenReturn(listOf(NomisDpsLocationMapping(education2DpsLocation.id, 3)))

      service.getInternalLocationsMapByIds(
        prisonCode,
        setOf(education2DpsLocation.id),
      ) isEqualTo mapOf(
        education2Location.locationId to education2LocationDetails,
      )
    }
  }

  @Nested
  @DisplayName("getInternalLocationEventsSummaries")
  inner class GetInternalLocationEventsSummaries {
    @BeforeEach
    fun setUp() {
      addCaseloadIdToRequestHeader(prisonCode)
    }

    @Test
    fun `uses events from all day when no time slot is supplied`() = runBlocking {
      whenever(
        prisonerScheduledActivityRepository.findByPrisonCodeAndDateAndTimeSlot(
          prisonCode,
          date,
          null,
        ),
      ).thenReturn(listOf(education2Activity))

      whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(education1Appointment))

      whenever(prisonApiClient.getEventLocationsBookedAsync(prisonCode, date, null))
        .thenReturn(
          listOf(
            education1LocationSummary,
            education2LocationSummary,
            education3LocationSummary,
            socialVisitsLocationSummary,
          ),
        )

      whenever(
        prisonApiClient.getScheduledVisitsForLocationAsync(
          prisonCode,
          education1LocationSummary.locationId,
          date,
          null,
        ),
      )
        .thenReturn(listOf(education1Visit))

      whenever(
        prisonApiClient.getScheduledVisitsForLocationAsync(
          prisonCode,
          education2LocationSummary.locationId,
          date,
          null,
        ),
      )
        .thenReturn(listOf(education2Visit))

      whenever(
        prisonApiClient.getScheduledVisitsForLocationAsync(
          prisonCode,
          education3LocationSummary.locationId,
          date,
          null,
        ),
      )
        .thenReturn(emptyList())

      whenever(
        prisonApiClient.getScheduledVisitsForLocationAsync(
          prisonCode,
          socialVisitsLocationSummary.locationId,
          date,
          null,
        ),
      )
        .thenReturn(listOf(socialVisit))

      whenever(adjudicationsHearingAdapter.getAdjudicationsByLocation(any(), any(), anyOrNull(), any())).thenReturn(
        mapOf(adjudicationLocation.locationId to listOf(adjudicationHearing)),
      )

      val dpsLocationId2 = UUID.fromString("22222222-2222-2222-2222-222222222222")
      val dpsLocationId3 = UUID.fromString("33333333-3333-3333-3333-333333333333")
      val dpsLocationId5 = UUID.fromString("55555555-5555-5555-5555-555555555555")
      val dpsLocationId1000 = UUID.fromString("10001000-1000-1000-1000-100010001000")

      whenever(nomisMappingAPIClient.getLocationMappingsByNomisIds(setOf(2, 3, 5, 1000))).thenReturn(
        listOf(
          NomisDpsLocationMapping(dpsLocationId2, 2),
          NomisDpsLocationMapping(dpsLocationId3, 3),
          NomisDpsLocationMapping(dpsLocationId5, 5),
          NomisDpsLocationMapping(dpsLocationId1000, 1000),
        ),
      )

      val dpsLocation2 = dpsLocation(dpsLocationId2, "MDI", "L2", "Location MDI 2")
      val dpsLocation3 = dpsLocation(dpsLocationId3, "MDI", "L3", "Location MDI 3")
      val dpsLocation5 = dpsLocation(dpsLocationId5, "MDI", "L5", "Location MDI 5")
      val dpsLocation1000 = dpsLocation(dpsLocationId1000, "MDI", "L1000", "Location MDI 100")

      whenever(locationsInsidePrisonAPIClient.getNonResidentialLocations(prisonCode)).thenReturn(
        listOf(
          dpsLocation2,
          dpsLocation3,
          dpsLocation5,
          dpsLocation1000,
        ),
      )

      service.getInternalLocationEventsSummaries(
        prisonCode,
        date,
        null,
      ) isEqualTo setOf(
        InternalLocationEventsSummary(
          education1Location.locationId,
          dpsLocationId2,
          prisonCode,
          dpsLocation2.code,
          dpsLocation2.localName!!,
        ),
        InternalLocationEventsSummary(
          education2Location.locationId,
          dpsLocationId3,
          prisonCode,
          dpsLocation3.code,
          dpsLocation3.localName!!,
        ),
        InternalLocationEventsSummary(
          socialVisitsLocation.locationId,
          dpsLocationId5,
          prisonCode,
          dpsLocation5.code,
          dpsLocation5.localName!!,
        ),
        InternalLocationEventsSummary(
          adjudicationLocation.locationId,
          dpsLocationId1000,
          prisonCode,
          dpsLocation1000.code,
          dpsLocation1000.localName!!,
        ),
      )

      verify(appointmentSearchSpecification).prisonCodeEquals(prisonCode)
      verify(appointmentSearchSpecification).startDateEquals(date)
      verify(appointmentSearchSpecification).startTimeBetween(LocalTime.of(0, 0), LocalTime.of(23, 59))
      verifyNoMoreInteractions(appointmentSearchSpecification)
    }

    @Test
    fun `uses events from time slot when time slot is supplied`() = runBlocking {
      whenever(
        prisonerScheduledActivityRepository.findByPrisonCodeAndDateAndTimeSlot(
          prisonCode,
          date,
          TimeSlot.PM,
        ),
      ).thenReturn(listOf(inactiveEducation1Activity))
      whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(noUserDescriptionLocationAppointment))
      whenever(prisonApiClient.getEventLocationsBookedAsync(prisonCode, date, TimeSlot.PM)).thenReturn(
        listOf(
          socialVisitsLocationSummary,
        ),
      )
      whenever(
        prisonApiClient.getScheduledVisitsForLocationAsync(
          prisonCode,
          socialVisitsLocationSummary.locationId,
          date,
          TimeSlot.PM,
        ),
      )
        .thenReturn(listOf(socialVisit))

      val dpsLocationId1 = UUID.fromString("11111111-1111-1111-1111-111111111111")
      val dpsLocationId4 = UUID.fromString("44444444-4444-4444-4444-444444444444")
      val dpsLocationId5 = UUID.fromString("55555555-5555-5555-5555-555555555555")
      val dpsLocationId1000 = UUID.fromString("10001000-1000-1000-1000-100010001000")

      whenever(nomisMappingAPIClient.getLocationMappingsByNomisIds(setOf(1, 4, 5, 1000))).thenReturn(
        listOf(
          NomisDpsLocationMapping(dpsLocationId1, 1),
          NomisDpsLocationMapping(dpsLocationId4, 4),
          NomisDpsLocationMapping(dpsLocationId5, 5),
          NomisDpsLocationMapping(dpsLocationId1000, 1000),
        ),
      )

      val dpsLocation1 = dpsLocation(dpsLocationId1, "MDI", "L1", "Location MDI 1")
      val dpsLocation4 = dpsLocation(dpsLocationId4, "MDI", "L4", "Location MDI 4")
      val dpsLocation5 = dpsLocation(dpsLocationId5, "MDI", "L5", "Location MDI 5")
      val dpsLocation1000 = dpsLocation(dpsLocationId1000, "MDI", "L1000", "Location MDI 100")

      whenever(locationsInsidePrisonAPIClient.getNonResidentialLocations(prisonCode)).thenReturn(
        listOf(
          dpsLocation1,
          dpsLocation4,
          dpsLocation5,
          dpsLocation1000,
        ),
      )

      service.getInternalLocationEventsSummaries(
        prisonCode,
        date,
        TimeSlot.PM,
      ) isEqualTo setOf(
        InternalLocationEventsSummary(
          noUserDescriptionLocation.locationId,
          dpsLocationId4,
          prisonCode,
          dpsLocation4.code,
          dpsLocation4.localName!!,
        ),
        InternalLocationEventsSummary(
          inactiveEducation1Location.locationId,
          dpsLocationId1,
          prisonCode,
          dpsLocation1.code,
          dpsLocation1.localName!!,
        ),
        InternalLocationEventsSummary(
          socialVisitsLocation.locationId,
          dpsLocationId5,
          prisonCode,
          dpsLocation5.code,
          dpsLocation5.localName!!,
        ),
        InternalLocationEventsSummary(
          adjudicationLocation.locationId,
          dpsLocationId1000,
          prisonCode,
          dpsLocation1000.code,
          dpsLocation1000.localName!!,
        ),
      )

      verify(appointmentSearchSpecification).prisonCodeEquals(prisonCode)
      verify(appointmentSearchSpecification).startDateEquals(date)
      verify(appointmentSearchSpecification).startTimeBetween(timeSlotPm.first, timeSlotPm.second.minusMinutes(1))
      verifyNoMoreInteractions(appointmentSearchSpecification)
    }

    @Test
    fun `exclude events with no internal location ids`() = runBlocking {
      whenever(
        prisonerScheduledActivityRepository.findByPrisonCodeAndDateAndTimeSlot(
          prisonCode,
          date,
          TimeSlot.PM,
        ),
      ).thenReturn(listOf(noLocationActivity))
      whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(noLocationAppointment))
      whenever(prisonApiClient.getEventLocationsBookedAsync(prisonCode, date, TimeSlot.AM)).thenReturn(emptyList())
      whenever(adjudicationsHearingAdapter.getAdjudicationsByLocation(any(), any(), anyOrNull(), any())).thenReturn(
        emptyMap(),
      )

      whenever(nomisMappingAPIClient.getLocationMappingsByNomisIds(emptySet())).thenReturn(emptyList<NomisDpsLocationMapping>())

      whenever(locationsInsidePrisonAPIClient.getNonResidentialLocations(prisonCode)).thenReturn(
        listOf(
          dpsLocation(UUID.randomUUID(), "MDI", "Other place"),
        ),
      )

      service.getInternalLocationEventsSummaries(
        prisonCode,
        date,
        TimeSlot.AM,
      ) isEqualTo emptySet()
    }

    @Test
    fun `exclude events where only allocations ended today`() = runBlocking {
      val activityWithNoAttendance = activityFromDbInstance(
        scheduledInstanceId = 4,
        sessionDate = LocalDate.now(),
        attendanceStatus = null,
      )

      whenever(
        prisonerScheduledActivityRepository.findByPrisonCodeAndDateAndTimeSlot(
          prisonCode,
          date,
          TimeSlot.AM,
        ),
      ).thenReturn(listOf(activityWithNoAttendance))
      whenever(appointmentSearchRepository.findAll(any())).thenReturn(emptyList())
      whenever(prisonApiClient.getEventLocationsBookedAsync(any(), any(), any())).thenReturn(emptyList())
      whenever(adjudicationsHearingAdapter.getAdjudicationsByLocation(any(), any(), anyOrNull(), any())).thenReturn(
        emptyMap(),
      )

      whenever(nomisMappingAPIClient.getLocationMappingsByNomisIds(emptySet())).thenReturn(emptyList<NomisDpsLocationMapping>())

      whenever(locationsInsidePrisonAPIClient.getNonResidentialLocations(prisonCode)).thenReturn(
        listOf(
          dpsLocation(UUID.randomUUID(), "MDI", "Other place"),
        ),
      )

      service.getInternalLocationEventsSummaries(
        prisonCode,
        date,
        TimeSlot.AM,
      ) isEqualTo emptySet()
    }
  }

  @Nested
  @DisplayName("getInternalLocationEvents")
  inner class GetInternalLocationEvents {
    @BeforeEach
    fun setUp() {
      addCaseloadIdToRequestHeader(prisonCode)

      whenever(prisonRegimeService.getEventPrioritiesForPrison(prisonCode))
        .thenReturn(EventPriorities(EventType.entries.associateWith { listOf(Priority(it.defaultPriority)) }))
    }

    @Test
    fun `uses events from all day when no time slot is supplied`() = runBlocking {
      val internalLocationIds = setOf(education1Location.locationId)

      whenever(
        prisonerScheduledActivityRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTimeSlot(
          prisonCode,
          internalLocationIds.map { it.toInt() }.toSet(),
          date,
          null,
        ),
      ).thenReturn(listOf(education1Activity))
      whenever(
        appointmentInstanceRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTime(
          prisonCode,
          internalLocationIds,
          date,
          LocalTime.of(0, 0),
          LocalTime.of(23, 59),
        ),
      ).thenReturn(listOf(education1AppointmentInstance))
      whenever(
        prisonApiClient.getScheduledVisitsForLocationAsync(
          prisonCode,
          education1Location.locationId,
          date,
          null,
        ),
      ).thenReturn(listOf(education1Visit))
      whenever(adjudicationsHearingAdapter.getAdjudicationsByLocation(any(), any(), anyOrNull(), any())).thenReturn(
        mapOf(adjudicationHearingForEvent.internalLocationId to listOf(adjudicationHearingForEvent)),
      )

      val dpsLocationId2 = UUID.fromString("22222222-2222-2222-2222-222222222222")

      whenever(nomisMappingAPIClient.getLocationMappingsByNomisIds(setOf(2))).thenReturn(
        listOf(
          NomisDpsLocationMapping(dpsLocationId2, education1Location.locationId),
        ),
      )

      whenever(nomisMappingAPIClient.getLocationMappingsByDpsIds(setOf(education1DpsLocation.id))).thenReturn(listOf(NomisDpsLocationMapping(education1DpsLocation.id, 2)))

      val result = service.getInternalLocationEvents(
        prisonCode,
        setOf(education1Location.locationId),
        date,
        null,
      )

      with(result) {
        size isEqualTo 1
        with(this.first()) {
          id isEqualTo education1Location.locationId
          prisonCode isEqualTo prisonCode
          code isEqualTo education1Location.description
          description isEqualTo education1Location.userDescription
          with(events) {
            size isEqualTo 4
            this.single { it.scheduledInstanceId == education1Activity.scheduledInstanceId }.eventType isEqualTo "ACTIVITY"
            this.single { it.appointmentAttendeeId == education1AppointmentInstance.appointmentAttendeeId }.eventType isEqualTo "APPOINTMENT"
            this.single { it.eventId == education1Visit.eventId }.eventType isEqualTo "VISIT"
            this.any { it.eventType == EventType.ADJUDICATION_HEARING.name } isEqualTo true
          }
        }
      }
    }

    @Test
    fun `will ignore activities for today where prisoner has no attendance records due to deallocation`() = runBlocking {
      val today = LocalDate.now()
      val internalLocationIds = setOf(education1Location.locationId)

      val activity1 = activityFromDbInstance(
        scheduledInstanceId = 1,
        internalLocationId = education1Location.locationId.toInt(),
        prisonerNumber = "AA1111A",
        sessionDate = today,
        attendanceStatus = null,
        startTime = LocalTime.of(8, 30),
        endTime = LocalTime.of(11, 45),
      )

      val activity2 = activityFromDbInstance(
        scheduledInstanceId = 2,
        internalLocationId = education1Location.locationId.toInt(),
        prisonerNumber = "BB2222B",
        sessionDate = today,
        attendanceStatus = AttendanceStatus.COMPLETED,
        startTime = LocalTime.of(8, 30),
        endTime = LocalTime.of(11, 45),
      )

      whenever(
        prisonerScheduledActivityRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTimeSlot(
          prisonCode,
          internalLocationIds.map { it.toInt() }.toSet(),
          date,
          null,
        ),
      ).thenReturn(listOf(activity1, activity2))

      whenever(appointmentInstanceRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTime(any(), any(), any(), any(), any())).thenReturn(emptyList())
      whenever(prisonApiClient.getScheduledVisitsForLocationAsync(any(), any(), any(), anyOrNull())).thenReturn(emptyList())
      whenever(adjudicationsHearingAdapter.getAdjudicationsByLocation(any(), any(), anyOrNull(), any())).thenReturn(emptyMap())

      val dpsLocationId2 = UUID.fromString("22222222-2222-2222-2222-222222222222")

      whenever(nomisMappingAPIClient.getLocationMappingsByNomisIds(setOf(2))).thenReturn(
        listOf(
          NomisDpsLocationMapping(dpsLocationId2, education1Location.locationId),
        ),
      )

      whenever(nomisMappingAPIClient.getLocationMappingsByDpsIds(setOf(education1DpsLocation.id))).thenReturn(listOf(NomisDpsLocationMapping(education1DpsLocation.id, 2)))

      val result = service.getInternalLocationEvents(prisonCode, setOf(education1Location.locationId), date, null)

      with(result) {
        size isEqualTo 1
        with(this.first()) {
          id isEqualTo education1Location.locationId
          prisonCode isEqualTo prisonCode
          code isEqualTo education1Location.description
          description isEqualTo education1Location.userDescription
          events.size isEqualTo 1

          with(events.first()) {
            scheduledInstanceId isEqualTo activity2.scheduledInstanceId
          }
        }
      }
    }

    @Test
    fun `uses events from time slot when time slot is supplied`() = runBlocking {
      val internalLocationIds = setOf(education2Location.locationId)

      whenever(
        prisonerScheduledActivityRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTimeSlot(
          prisonCode,
          internalLocationIds.map { it.toInt() }.toSet(),
          date,
          TimeSlot.PM,
        ),
      ).thenReturn(listOf(education2Activity))
      whenever(
        appointmentInstanceRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTime(
          prisonCode,
          internalLocationIds,
          date,
          timeSlotPm.first,
          timeSlotPm.second.minusMinutes(1),
        ),
      ).thenReturn(listOf(education2AppointmentInstance))
      whenever(
        prisonApiClient.getScheduledVisitsForLocationAsync(
          prisonCode,
          education2Location.locationId,
          date,
          TimeSlot.PM,
        ),
      ).thenReturn(listOf(education2Visit))

      val dpsLocationId3 = UUID.fromString("33333333-3333-3333-3333-333333333333")

      whenever(nomisMappingAPIClient.getLocationMappingsByNomisIds(setOf(3))).thenReturn(
        listOf(
          NomisDpsLocationMapping(dpsLocationId3, education2Location.locationId),
        ),
      )

      whenever(nomisMappingAPIClient.getLocationMappingsByDpsIds(setOf(education2DpsLocation.id))).thenReturn(listOf(NomisDpsLocationMapping(education2DpsLocation.id, 3)))

      val result = service.getInternalLocationEvents(
        prisonCode,
        setOf(education2Location.locationId),
        date,
        TimeSlot.PM,
      )

      with(result) {
        size isEqualTo 1
        with(this.first()) {
          id isEqualTo education2Location.locationId
          prisonCode isEqualTo prisonCode
          code isEqualTo education2DpsLocation.code
          description isEqualTo education2DpsLocation.localName
          with(events) {
            size isEqualTo 3
            this.single { it.scheduledInstanceId == education2Activity.scheduledInstanceId }.eventType isEqualTo "ACTIVITY"
            this.single { it.appointmentAttendeeId == education2AppointmentInstance.appointmentAttendeeId }.eventType isEqualTo "APPOINTMENT"
            this.single { it.eventId == education2Visit.eventId }.eventType isEqualTo "VISIT"
          }
        }
      }
    }

    @Test
    fun `exclude events with no internal location ids`() = runBlocking {
      val internalLocationIds = setOf(education1Location.locationId)

      whenever(
        prisonerScheduledActivityRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTimeSlot(
          prisonCode,
          internalLocationIds.map { it.toInt() }.toSet(),
          date,
          TimeSlot.ED,
        ),
      ).thenReturn(listOf(noLocationActivity))
      whenever(
        appointmentInstanceRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTime(
          prisonCode,
          internalLocationIds,
          date,
          timeSlotEd.first,
          timeSlotEd.second.minusMinutes(1),
        ),
      ).thenReturn(listOf(noLocationAppointmentInstance))
      whenever(
        prisonApiClient.getScheduledVisitsForLocationAsync(
          prisonCode,
          education1Location.locationId,
          date,
          TimeSlot.ED,
        ),
      ).thenReturn(listOf(noLocationVisit))

      val dpsLocationId2 = UUID.fromString("22222222-2222-2222-2222-222222222222")

      whenever(nomisMappingAPIClient.getLocationMappingsByNomisIds(setOf(2))).thenReturn(
        listOf(
          NomisDpsLocationMapping(dpsLocationId2, education1Location.locationId),
        ),
      )

      whenever(nomisMappingAPIClient.getLocationMappingsByDpsIds(setOf(education1DpsLocation.id))).thenReturn(listOf(NomisDpsLocationMapping(education1DpsLocation.id, 2)))

      val result = service.getInternalLocationEvents(
        prisonCode,
        setOf(education1Location.locationId),
        date,
        TimeSlot.ED,
      )

      with(result) {
        size isEqualTo 1
        with(this.first()) {
          id isEqualTo education1Location.locationId
          prisonCode isEqualTo prisonCode
          code isEqualTo education1Location.description
          description isEqualTo education1Location.userDescription
          events hasSize 0
        }
      }
    }
  }

  @Nested
  @DisplayName("caseload checks")
  inner class CaseloadChecks {
    @Test
    fun `getInternalLocationEventsSummaries throws caseload access exception if caseload id header does not match`() {
      addCaseloadIdToRequestHeader("WRONG")
      assertThatThrownBy { runBlocking { service.getInternalLocationEventsSummaries(prisonCode, LocalDate.now(), null) } }
        .isInstanceOf(CaseloadAccessException::class.java)
    }

    @Test
    fun `getInternalLocationEvents throws caseload access exception if caseload id header does not match`() {
      addCaseloadIdToRequestHeader("WRONG")
      assertThatThrownBy { service.getInternalLocationEvents(prisonCode, setOf(1L), LocalDate.now(), null) }
        .isInstanceOf(CaseloadAccessException::class.java)
    }
  }
}
