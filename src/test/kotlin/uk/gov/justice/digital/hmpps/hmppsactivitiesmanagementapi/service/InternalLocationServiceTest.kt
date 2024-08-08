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
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.adjudications.AdjudicationsHearingAdapter
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.LocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalTimeRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityFromDbInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentInstanceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity
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
  private val socialVisitsLocation = internalLocation(
    locationId = 5L,
    description = "SOCIAL VISITS",
    userDescription = "Social Visits",
  )
  private val socialVisitsLocationSummary = LocationSummary(
    locationId = 5L,
    description = "SOCIAL VISITS",
    userDescription = "Social Visits",
  )

  private val adjudicationLocation = internalLocation(
    locationId = 1000L,
    description = "ADJU",
    userDescription = "ADJU",
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
    prisonApiClient.stub {
      on {
        runBlocking {
          getEventLocationsAsync(prisonCode)
        }
      } doReturn listOf(
        education1Location,
        education2Location,
        noUserDescriptionLocation,
        socialVisitsLocation,
        adjudicationLocation,
      )

      on {
        runBlocking {
          getLocationAsync(inactiveEducation1Location.locationId, true)
        }
      } doReturn inactiveEducation1Location

      on {
        runBlocking {
          getLocationAsync(-1, true)
        }
      } doThrow WebClientResponseException(404, "", null, null, null)
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
          getAdjudicationsByLocation(any(), any(), anyOrNull())
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
      service.getInternalLocationsMapByIds(
        prisonCode,
        setOf(education2Location.locationId),
      ) isEqualTo mapOf(
        education2Location.locationId to education2Location,
      )
    }

    @Test
    fun `retrieves inactive locations matching supplied ids`() = runBlocking {
      service.getInternalLocationsMapByIds(
        prisonCode,
        setOf(
          inactiveEducation1Location.locationId, adjudicationLocation.locationId,
        ),
      ) isEqualTo mapOf(
        inactiveEducation1Location.locationId to inactiveEducation1Location,
        adjudicationLocation.locationId to adjudicationLocation,
      )
    }

    @Test
    fun `retrieves inactive locations and combines with those matching supplied ids`() = runBlocking {
      service.getInternalLocationsMapByIds(
        prisonCode,
        setOf(
          inactiveEducation1Location.locationId,
          education2Location.locationId,
        ),
      ) isEqualTo mapOf(
        inactiveEducation1Location.locationId to inactiveEducation1Location,
        education2Location.locationId to education2Location,
      )
    }

    @Test
    fun `tries to retrieve inactive locations matching supplied ids and catches errors`() = runBlocking {
      service.getInternalLocationsMapByIds(
        prisonCode,
        setOf(
          -1,
          education2Location.locationId,
        ),
      ) isEqualTo mapOf(
        education2Location.locationId to education2Location,
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
        .thenReturn(listOf(education1LocationSummary, education2LocationSummary, socialVisitsLocationSummary))
      whenever(adjudicationsHearingAdapter.getAdjudicationsByLocation(any(), any(), anyOrNull())).thenReturn(
        mapOf(adjudicationLocation.locationId to listOf(adjudicationHearing)),
      )

      service.getInternalLocationEventsSummaries(
        prisonCode,
        date,
        null,
      ) isEqualTo setOf(
        InternalLocationEventsSummary(
          education1Location.locationId,
          prisonCode,
          education1Location.description,
          education1Location.userDescription!!,
        ),
        InternalLocationEventsSummary(
          education2Location.locationId,
          prisonCode,
          education2Location.description,
          education2Location.userDescription!!,
        ),
        InternalLocationEventsSummary(
          socialVisitsLocation.locationId,
          prisonCode,
          socialVisitsLocation.description,
          socialVisitsLocation.userDescription!!,
        ),
        InternalLocationEventsSummary(
          adjudicationLocation.locationId,
          prisonCode,
          adjudicationLocation.description,
          adjudicationLocation.userDescription!!,
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

      service.getInternalLocationEventsSummaries(
        prisonCode,
        date,
        TimeSlot.PM,
      ) isEqualTo setOf(
        InternalLocationEventsSummary(
          noUserDescriptionLocation.locationId,
          prisonCode,
          noUserDescriptionLocation.description,
          noUserDescriptionLocation.description,
        ),
        InternalLocationEventsSummary(
          inactiveEducation1Location.locationId,
          prisonCode,
          inactiveEducation1Location.description,
          inactiveEducation1Location.userDescription!!,
        ),
        InternalLocationEventsSummary(
          socialVisitsLocation.locationId,
          prisonCode,
          socialVisitsLocation.description,
          socialVisitsLocation.userDescription!!,
        ),
        InternalLocationEventsSummary(
          adjudicationLocation.locationId,
          prisonCode,
          adjudicationLocation.description,
          adjudicationLocation.userDescription!!,
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
      whenever(adjudicationsHearingAdapter.getAdjudicationsByLocation(any(), any(), anyOrNull())).thenReturn(emptyMap())

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
      whenever(adjudicationsHearingAdapter.getAdjudicationsByLocation(any(), any(), anyOrNull())).thenReturn(
        mapOf(adjudicationHearingForEvent.internalLocationId to listOf(adjudicationHearingForEvent)),
      )

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
          code isEqualTo education2Location.description
          description isEqualTo education2Location.userDescription
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
      assertThatThrownBy { service.getInternalLocationEventsSummaries(prisonCode, LocalDate.now(), null) }
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
