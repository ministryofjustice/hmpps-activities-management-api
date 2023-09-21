package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalTimeRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityFromDbInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentInstanceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.internalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocationEventsSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSearchSpecification
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.time.LocalDate
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

  val service = InternalLocationService(
    appointmentAttendeeSearchRepository,
    appointmentInstanceRepository,
    appointmentSearchRepository,
    appointmentSearchSpecification,
    prisonApiClient,
    prisonerScheduledActivityRepository,
    prisonRegimeService,
    referenceCodeService,
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
  private val education2Location = internalLocation(
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

  @BeforeEach
  fun setUp() {
    prisonApiClient.stub {
      on {
        runBlocking {
          getEventLocationsAsync(prisonCode)
        }
      } doReturn listOf(education1Location, education2Location, noUserDescriptionLocation)

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
        getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.AM)
      } doReturn LocalTimeRange(timeSlotAm.first, timeSlotAm.second)
      on {
        getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.PM)
      } doReturn LocalTimeRange(timeSlotPm.first, timeSlotPm.second)
      on {
        getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.ED)
      } doReturn LocalTimeRange(timeSlotEd.first, timeSlotEd.second)
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
          inactiveEducation1Location.locationId,
        ),
      ) isEqualTo mapOf(
        inactiveEducation1Location.locationId to inactiveEducation1Location,
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
        prisonerScheduledActivityRepository.findByPrisonCodeAndDateAndTime(
          prisonCode,
          date,
          LocalTime.of(0, 0),
          LocalTime.of(23, 59),
        ),
      ).thenReturn(listOf(education2Activity))
      whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(education1Appointment))

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
      )

      verify(appointmentSearchSpecification).prisonCodeEquals(prisonCode)
      verify(appointmentSearchSpecification).startDateEquals(date)
      verify(appointmentSearchSpecification).startTimeBetween(LocalTime.of(0, 0), LocalTime.of(23, 59))
      verifyNoMoreInteractions(appointmentSearchSpecification)
    }

    @Test
    fun `uses events from time slot when time slot is supplied`() = runBlocking {
      whenever(
        prisonerScheduledActivityRepository.findByPrisonCodeAndDateAndTime(
          prisonCode,
          date,
          timeSlotPm.first,
          timeSlotPm.second,
        ),
      ).thenReturn(listOf(inactiveEducation1Activity))
      whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(noUserDescriptionLocationAppointment))

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
      )

      verify(appointmentSearchSpecification).prisonCodeEquals(prisonCode)
      verify(appointmentSearchSpecification).startDateEquals(date)
      verify(appointmentSearchSpecification).startTimeBetween(timeSlotPm.first, timeSlotPm.second)
      verifyNoMoreInteractions(appointmentSearchSpecification)
    }

    @Test
    fun `exclude events with no internal location ids`() = runBlocking {
      whenever(
        prisonerScheduledActivityRepository.findByPrisonCodeAndDateAndTime(
          prisonCode,
          date,
          timeSlotPm.first,
          timeSlotPm.second,
        ),
      ).thenReturn(listOf(noLocationActivity))
      whenever(appointmentSearchRepository.findAll(any())).thenReturn(listOf(noLocationAppointment))

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
        prisonerScheduledActivityRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTime(
          prisonCode,
          internalLocationIds.map { it.toInt() }.toSet(),
          date,
          LocalTime.of(0, 0),
          LocalTime.of(23, 59),
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
            size isEqualTo 2
            with(this.single { it.scheduledInstanceId == education1Activity.scheduledInstanceId }) {
              eventType isEqualTo "ACTIVITY"
            }
            with(this.single { it.appointmentAttendeeId == education1AppointmentInstance.appointmentAttendeeId }) {
              appointmentAttendeeId isEqualTo education1AppointmentInstance.appointmentInstanceId
              eventType isEqualTo "APPOINTMENT"
            }
          }
        }
      }
    }

    @Test
    fun `uses events from time slot when time slot is supplied`() = runBlocking {
      val internalLocationIds = setOf(education2Location.locationId)

      whenever(
        prisonerScheduledActivityRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTime(
          prisonCode,
          internalLocationIds.map { it.toInt() }.toSet(),
          date,
          timeSlotPm.first,
          timeSlotPm.second,
        ),
      ).thenReturn(listOf(education2Activity))
      whenever(
        appointmentInstanceRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTime(
          prisonCode,
          internalLocationIds,
          date,
          timeSlotPm.first,
          timeSlotPm.second,
        ),
      ).thenReturn(listOf(education2AppointmentInstance))

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
            size isEqualTo 2
            with(this.single { it.scheduledInstanceId == education2Activity.scheduledInstanceId }) {
              eventType isEqualTo "ACTIVITY"
            }
            with(this.single { it.appointmentAttendeeId == education2AppointmentInstance.appointmentAttendeeId }) {
              appointmentAttendeeId isEqualTo education2AppointmentInstance.appointmentInstanceId
              eventType isEqualTo "APPOINTMENT"
            }
          }
        }
      }
    }

    @Test
    fun `exclude events with no internal location ids`() = runBlocking {
      val internalLocationIds = setOf(education2Location.locationId)

      whenever(
        prisonerScheduledActivityRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTime(
          prisonCode,
          internalLocationIds.map { it.toInt() }.toSet(),
          date,
          timeSlotEd.first,
          timeSlotEd.second,
        ),
      ).thenReturn(listOf(noLocationActivity))
      whenever(
        appointmentInstanceRepository.findByPrisonCodeAndInternalLocationIdsAndDateAndTime(
          prisonCode,
          internalLocationIds,
          date,
          timeSlotEd.first,
          timeSlotEd.second,
        ),
      ).thenReturn(listOf(noLocationAppointmentInstance))

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
