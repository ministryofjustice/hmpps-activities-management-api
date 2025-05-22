package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AdvanceAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.advanceAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.schedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AdvanceAttendanceCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AdvanceAttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonerScheduledActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AdvanceAttendance as ModelAdvanceAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AdvanceAttendanceHistory as ModelAdvanceAttendanceHistory

class AdvanceAttendanceServiceTest {
  private val scheduledInstanceRepository: ScheduledInstanceRepository = mock()
  private val advanceAttendanceRepository: AdvanceAttendanceRepository = mock()
  private val prisonerScheduledActivityRepository: PrisonerScheduledActivityRepository = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val service = AdvanceAttendanceService(
    scheduledInstanceRepository,
    advanceAttendanceRepository,
    prisonerScheduledActivityRepository,
    prisonerSearchApiClient,
    allocationRepository,
  )

  val captor = argumentCaptor<AdvanceAttendance>()

  @BeforeEach
  fun setUp() {
    addCaseloadIdToRequestHeader(MOORLAND_PRISON_CODE)
  }

  @Nested
  inner class GetAttendanceById {

    @Test
    fun `should retrieve advance attendance`() {
      val attendance = advanceAttendance()

      whenever(advanceAttendanceRepository.findById(124)).thenReturn(Optional.of(attendance))

      val activityPay = attendance.scheduledInstance.activitySchedule.activity.activityPay().first()

      val thePayBand = activityPay.payBand

      val incentiveLevel = activityPay.incentiveNomisCode

      val incentive: CurrentIncentive = mock { on { level } doReturn IncentiveLevel("description", incentiveLevel) }

      val prisoner: Prisoner = mock { on { currentIncentive } doReturn incentive }

      whenever(prisonerSearchApiClient.findByPrisonerNumber(attendance.prisonerNumber)).thenReturn(prisoner)

      val prisonerScheduledActivity: PrisonerScheduledActivity = mock { on { allocationId } doReturn 124 }

      whenever(
        prisonerScheduledActivityRepository.getByScheduledInstanceIdAndPrisonerNumber(
          attendance.scheduledInstance.scheduledInstanceId,
          attendance.prisonerNumber,
        ),
      ).thenReturn(Optional.of(prisonerScheduledActivity))

      val allocation: Allocation = mock { on { payBand } doReturn thePayBand }

      whenever(allocationRepository.findById(124)).thenReturn(Optional.of(allocation))

      val expected = ModelAdvanceAttendance(
        id = attendance.advanceAttendanceId,
        scheduleInstanceId = attendance.scheduledInstance.scheduledInstanceId,
        prisonerNumber = attendance.prisonerNumber,
        issuePayment = attendance.issuePayment,
        payAmount = activityPay.rate,
        recordedTime = attendance.recordedTime,
        recordedBy = attendance.recordedBy,
        attendanceHistory = listOf(
          ModelAdvanceAttendanceHistory(
            id = attendance.history().last().advanceAttendanceHistoryId,
            issuePayment = attendance.history().last().issuePayment,
            recordedTime = attendance.history().last().recordedTime,
            recordedBy = attendance.history().last().recordedBy,
          ),
          ModelAdvanceAttendanceHistory(
            id = attendance.history().first().advanceAttendanceHistoryId,
            issuePayment = attendance.history().first().issuePayment,
            recordedTime = attendance.history().first().recordedTime,
            recordedBy = attendance.history().first().recordedBy,
          ),
        ),
      )

      assertThat(service.getAttendanceById(124)).isEqualTo(expected)
    }

    @Test
    fun `should throw exception if case load is invalid`() {
      val attendance = advanceAttendance(activity = activityEntity(prisonCode = "RSI"))

      whenever(advanceAttendanceRepository.findById(attendance.advanceAttendanceId)).thenReturn(Optional.of(attendance))

      assertThatThrownBy {
        service.getAttendanceById(attendance.advanceAttendanceId)
      }
        .isInstanceOf(CaseloadAccessException::class.java)

      verify(advanceAttendanceRepository, never()).save(any())
    }
  }

  @Nested
  inner class Create {
    lateinit var theScheduledInstance: ScheduledInstance

    @BeforeEach
    fun setUp() {
      val activity = activityEntity(noSchedules = true)
      val activitySchedule = activitySchedule(activity = activity, startDate = LocalDate.now().plusDays(14))
      activity.addSchedule(activitySchedule)
      theScheduledInstance = activitySchedule.instances().first()

      val activityPay = activity.activityPay().first()

      val thePayBand = activityPay.payBand

      val incentiveLevel = activityPay.incentiveNomisCode

      whenever(scheduledInstanceRepository.findById(theScheduledInstance.scheduledInstanceId)).thenReturn(
        Optional.of(
          theScheduledInstance,
        ),
      )

      val incentive: CurrentIncentive = mock { on { level } doReturn IncentiveLevel("description", incentiveLevel) }

      val prisoner: Prisoner = mock { on { currentIncentive } doReturn incentive }

      whenever(prisonerSearchApiClient.findByPrisonerNumber("B11111B")).thenReturn(prisoner)

      val prisonerScheduledActivity: PrisonerScheduledActivity = mock { on { allocationId } doReturn 124 }

      whenever(
        prisonerScheduledActivityRepository.getByScheduledInstanceIdAndPrisonerNumber(
          theScheduledInstance.scheduledInstanceId,
          "B11111B",
        ),
      ).thenReturn(Optional.of(prisonerScheduledActivity))

      val allocation: Allocation = mock { on { payBand } doReturn thePayBand }

      whenever(allocationRepository.findById(124)).thenReturn(Optional.of(allocation))

      val savedAdvanceAttendance = AdvanceAttendance(
        advanceAttendanceId = 345,
        scheduledInstance = theScheduledInstance,
        prisonerNumber = "B11111B",
        issuePayment = true,
        recordedTime = LocalDateTime.now(),
        recordedBy = "USER2",
      )

      whenever(advanceAttendanceRepository.save(any())).thenReturn(savedAdvanceAttendance)
    }

    @Test
    fun `should create advance attendance`() {
      val request = AdvanceAttendanceCreateRequest(theScheduledInstance.scheduledInstanceId, "B11111B", true)

      val result = service.create(request, "USER2")

      with(result) {
        id isEqualTo 345
        scheduleInstanceId isEqualTo theScheduledInstance.scheduledInstanceId
        prisonerNumber isEqualTo "B11111B"
        issuePayment isEqualTo true
        recordedTime isCloseTo LocalDateTime.now()
        recordedBy isEqualTo "USER2"
      }

      verify(advanceAttendanceRepository).save(captor.capture())

      with(captor.firstValue) {
        advanceAttendanceId isEqualTo 0
        scheduledInstance isEqualTo theScheduledInstance
        prisonerNumber isEqualTo "B11111B"
        issuePayment isEqualTo true
        recordedTime isCloseTo LocalDateTime.now()
        recordedBy isEqualTo "USER2"
      }
    }

    @Test
    fun `should throw exception if scheduled instances is not found`() {
      whenever(scheduledInstanceRepository.findById(theScheduledInstance.scheduledInstanceId)).thenThrow(
        EntityNotFoundException("Schedule instance not found"),
      )

      val request = AdvanceAttendanceCreateRequest(theScheduledInstance.scheduledInstanceId, "B11111B", true)

      assertThatThrownBy {
        service.create(request, "USER2")
      }
        .isInstanceOf(EntityNotFoundException::class.java)
        .hasMessage("Schedule instance not found")

      verifyNoInteractions(advanceAttendanceRepository)
    }

    @Test
    fun `should throw exception if case load is invalid`() {
      val activity = activityEntity(noSchedules = true, prisonCode = "RSI")
      val activitySchedule =
        activitySchedule(activity = activity, noAllocations = true, startDate = LocalDate.now().plusDays(1))
      activity.addSchedule(activitySchedule)
      val scheduledInstance = activitySchedule.instances().first()

      whenever(scheduledInstanceRepository.findById(scheduledInstance.scheduledInstanceId)).thenReturn(
        Optional.of(
          scheduledInstance,
        ),
      )

      val request = AdvanceAttendanceCreateRequest(theScheduledInstance.scheduledInstanceId, "B11111B", true)

      assertThatThrownBy {
        service.create(request, "USER2")
      }
        .isInstanceOf(CaseloadAccessException::class.java)

      verifyNoInteractions(advanceAttendanceRepository)
    }

    @Test
    fun `should throw exception if advance attendance already exists for the prisoner`() {
      val request = AdvanceAttendanceCreateRequest(
        theScheduledInstance.scheduledInstanceId,
        theScheduledInstance.advanceAttendances.first().prisonerNumber,
        true,
      )

      assertThatThrownBy {
        service.create(request, "USER2")
      }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Prisoner A1234AA already has an advance attendance record for this session")

      verifyNoInteractions(advanceAttendanceRepository)
    }

    @Test
    fun `should throw exception if advance attendance is not in the future`() {
      val scheduledInstance = activityEntity().schedule().instances().first()

      whenever(scheduledInstanceRepository.findById(scheduledInstance.scheduledInstanceId)).thenReturn(
        Optional.of(
          scheduledInstance,
        ),
      )

      val request = AdvanceAttendanceCreateRequest(theScheduledInstance.scheduledInstanceId, "B11111B", true)

      assertThatThrownBy {
        service.create(request, "USER2")
      }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Can only create an advance attendance for future dates")

      verifyNoInteractions(advanceAttendanceRepository)
    }

    @Test
    fun `should throw exception if advance attendance over 14 days into the future`() {
      val activity = activityEntity(noSchedules = true)
      val activitySchedule = activitySchedule(activity = activity, startDate = LocalDate.now().plusDays(15))
      activity.addSchedule(activitySchedule)
      val scheduledInstance = activitySchedule.instances().first()

      whenever(scheduledInstanceRepository.findById(scheduledInstance.scheduledInstanceId)).thenReturn(
        Optional.of(
          scheduledInstance,
        ),
      )

      val request = AdvanceAttendanceCreateRequest(theScheduledInstance.scheduledInstanceId, "B11111B", true)

      assertThatThrownBy {
        service.create(request, "USER2")
      }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Can only create an advance attendance for up to 14 days in advance")

      verifyNoInteractions(advanceAttendanceRepository)
    }

    @Test
    fun `should throw exception if attempting to issue payment on unpaid activity`() {
      val activity = activityEntity(noSchedules = true, paid = false, noPayBands = true)
      val activitySchedule =
        activitySchedule(activity = activity, noAllocations = true, startDate = LocalDate.now().plusDays(1))
      activity.addSchedule(activitySchedule)
      activitySchedule.allocatePrisoner(
        prisonerNumber = "A1234AA".toPrisonerNumber(),
        bookingId = 10001,
        payBand = null,
        allocatedBy = "Mr Blogs",
        startDate = activity.startDate,
      )
      val scheduledInstance = activitySchedule.instances().first()

      whenever(scheduledInstanceRepository.findById(scheduledInstance.scheduledInstanceId)).thenReturn(
        Optional.of(
          scheduledInstance,
        ),
      )

      val request = AdvanceAttendanceCreateRequest(theScheduledInstance.scheduledInstanceId, "B11111B", true)

      assertThatThrownBy {
        service.create(request, "USER2")
      }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot issue payment for an unpaid activity")

      verifyNoInteractions(advanceAttendanceRepository)
    }
  }

  @Nested
  inner class Update {
    lateinit var theActivity: Activity
    lateinit var theScheduledInstance: ScheduledInstance

    lateinit var theOriginalAdvanceAttendance: AdvanceAttendance

    @BeforeEach
    fun setUp() {
      theActivity = activityEntity(noSchedules = true)
      val activitySchedule = activitySchedule(activity = theActivity, startDate = LocalDate.now().plusDays(14))
      theActivity.addSchedule(activitySchedule)
      theScheduledInstance = activitySchedule.instances().first()

      val activityPay = theActivity.activityPay().first()

      val thePayBand = activityPay.payBand

      val incentiveLevel = activityPay.incentiveNomisCode

      whenever(scheduledInstanceRepository.findById(theScheduledInstance.scheduledInstanceId)).thenReturn(
        Optional.of(
          theScheduledInstance,
        ),
      )

      val incentive: CurrentIncentive = mock { on { level } doReturn IncentiveLevel("description", incentiveLevel) }

      val prisoner: Prisoner = mock { on { currentIncentive } doReturn incentive }

      whenever(prisonerSearchApiClient.findByPrisonerNumber("B11111B")).thenReturn(prisoner)

      val prisonerScheduledActivity: PrisonerScheduledActivity = mock { on { allocationId } doReturn 124 }

      whenever(
        prisonerScheduledActivityRepository.getByScheduledInstanceIdAndPrisonerNumber(
          theScheduledInstance.scheduledInstanceId,
          "B11111B",
        ),
      ).thenReturn(Optional.of(prisonerScheduledActivity))

      val allocation: Allocation = mock { on { payBand } doReturn thePayBand }

      whenever(allocationRepository.findById(124)).thenReturn(Optional.of(allocation))

      theOriginalAdvanceAttendance = AdvanceAttendance(
        advanceAttendanceId = 345,
        scheduledInstance = theScheduledInstance,
        prisonerNumber = "B11111B",
        issuePayment = false,
        recordedTime = LocalDateTime.now(),
        recordedBy = "USER2",
      )

      whenever(advanceAttendanceRepository.findById(theOriginalAdvanceAttendance.advanceAttendanceId)).thenReturn(
        Optional.of(theOriginalAdvanceAttendance),
      )
    }

    @Test
    fun `can update an advance attendance where the activity is paid`() {
      whenever(advanceAttendanceRepository.save(any())).thenReturn(theOriginalAdvanceAttendance.copy(issuePayment = true))

      val result = service.update(theOriginalAdvanceAttendance.advanceAttendanceId, true, "USER2")

      with(result) {
        id isEqualTo 345
        scheduleInstanceId isEqualTo theScheduledInstance.scheduledInstanceId
        prisonerNumber isEqualTo "B11111B"
        issuePayment isEqualTo true
        recordedTime isCloseTo LocalDateTime.now()
        recordedBy isEqualTo "USER2"
      }

      verify(advanceAttendanceRepository).save(captor.capture())

      with(captor.firstValue) {
        advanceAttendanceId isEqualTo 345
        scheduledInstance isEqualTo theScheduledInstance
        prisonerNumber isEqualTo "B11111B"
        issuePayment isEqualTo true
        recordedTime isCloseTo LocalDateTime.now()
        recordedBy isEqualTo "USER2"
      }
    }

    @Test
    fun `should throw exception if case load is invalid`() {
      val activity = activityEntity(noSchedules = true, prisonCode = "RSI")
      val activitySchedule =
        activitySchedule(activity = activity, noAllocations = true, startDate = LocalDate.now().plusDays(1))
      activity.addSchedule(activitySchedule)
      val advanceAttendance = activitySchedule.instances().first().advanceAttendances.first()

      whenever(advanceAttendanceRepository.findById(advanceAttendance.advanceAttendanceId)).thenReturn(
        Optional.of(
          advanceAttendance,
        ),
      )

      assertThatThrownBy {
        service.update(advanceAttendance.advanceAttendanceId, true, "USER2")
      }
        .isInstanceOf(CaseloadAccessException::class.java)

      verify(advanceAttendanceRepository, never()).save(any())
    }

    @Test
    fun `should throw exception if session is no longer in the future`() {
      val advanceAttendance = activityEntity().schedule().instances().first().advanceAttendances.first()

      whenever(advanceAttendanceRepository.findById(advanceAttendance.advanceAttendanceId)).thenReturn(
        Optional.of(
          advanceAttendance,
        ),
      )

      assertThatThrownBy {
        service.update(advanceAttendance.advanceAttendanceId, true, "USER2")
      }
        .isInstanceOf(java.lang.IllegalArgumentException::class.java)
        .hasMessage("Advance attendance can only be updated for future sessions")

      verify(advanceAttendanceRepository, never()).save(any())
    }

    @Test
    fun `should throw exception attempting to issue payment on unpaid activity`() {
      val activity = activityEntity(noSchedules = true, paid = false, noPayBands = true)
      val activitySchedule =
        activitySchedule(activity = activity, noAllocations = true, startDate = LocalDate.now().plusDays(1))
      activity.addSchedule(activitySchedule)
      val advanceAttendance = activitySchedule.instances().first().advanceAttendances.first()

      whenever(advanceAttendanceRepository.findById(advanceAttendance.advanceAttendanceId)).thenReturn(
        Optional.of(
          advanceAttendance,
        ),
      )

      assertThatThrownBy {
        service.update(advanceAttendance.advanceAttendanceId, true, "USER2")
      }
        .isInstanceOf(IllegalArgumentException::class.java)
        .hasMessage("Cannot issue payment for an unpaid activity")

      verify(advanceAttendanceRepository, never()).save(any())
    }
  }

  @Nested
  inner class Delete {
    @Test
    fun `can delete an advance attendance`() {
      val advanceAttendance = activityEntity().schedule().instances().first().advanceAttendances.first()

      whenever(advanceAttendanceRepository.findById(advanceAttendance.advanceAttendanceId)).thenReturn(Optional.of(advanceAttendance))

      service.delete(advanceAttendance.advanceAttendanceId)

      verify(advanceAttendanceRepository).delete(advanceAttendance)
    }

    @Test
    fun `should throw exception if case load is invalid`() {
      val advanceAttendance = activityEntity(prisonCode = "RSI").schedule().instances().first().advanceAttendances.first()

      whenever(advanceAttendanceRepository.findById(advanceAttendance.advanceAttendanceId)).thenReturn(Optional.of(advanceAttendance))

      assertThatThrownBy {
        service.delete(advanceAttendance.advanceAttendanceId)
      }
        .isInstanceOf(CaseloadAccessException::class.java)

      verify(advanceAttendanceRepository, never()).save(any())
    }
  }

  @Nested
  inner class DeterminePay {
    @Test
    fun `returns null if advance attendance has issuePayment set to false`() {
      val attendance = advanceAttendance(issuePayment = false)

      assertThat(service.determinePay(attendance)).isNull()
    }

    @Test
    fun `returns zero if activity is not paid`() {
      val attendance = advanceAttendance(activity = activityEntity(paid = false, noPayBands = true))

      assertThat(service.determinePay(attendance)).isZero()
    }

    @Test
    fun `returns pay rate when a pay rate is available`() {
      val attendance = advanceAttendance()

      val activityPay = attendance.scheduledInstance.activitySchedule.activity.activityPay().first()

      val thePayBand = activityPay.payBand

      val incentiveLevel = activityPay.incentiveNomisCode

      val incentive: CurrentIncentive = mock { on { level } doReturn IncentiveLevel("description", incentiveLevel) }

      val prisoner: Prisoner = mock { on { currentIncentive } doReturn incentive }

      whenever(prisonerSearchApiClient.findByPrisonerNumber(attendance.prisonerNumber)).thenReturn(prisoner)

      val prisonerScheduledActivity: PrisonerScheduledActivity = mock { on { allocationId } doReturn 124 }

      whenever(
        prisonerScheduledActivityRepository.getByScheduledInstanceIdAndPrisonerNumber(
          attendance.scheduledInstance.scheduledInstanceId,
          attendance.prisonerNumber,
        ),
      ).thenReturn(Optional.of(prisonerScheduledActivity))

      val allocation: Allocation = mock { on { payBand } doReturn thePayBand }

      whenever(allocationRepository.findById(124)).thenReturn(Optional.of(allocation))

      assertThat(service.determinePay(attendance)).isEqualTo(activityPay.rate)
    }

    @Test
    fun `returns zero if no activity pay with that incentive level code exists`() {
      val attendance = advanceAttendance()

      val activityPay = attendance.scheduledInstance.activitySchedule.activity.activityPay().first()

      val thePayBand = activityPay.payBand

      val incentive: CurrentIncentive = mock { on { level } doReturn IncentiveLevel("description", "STD") }

      val prisoner: Prisoner = mock { on { currentIncentive } doReturn incentive }

      whenever(prisonerSearchApiClient.findByPrisonerNumber(attendance.prisonerNumber)).thenReturn(prisoner)

      val prisonerScheduledActivity: PrisonerScheduledActivity = mock { on { allocationId } doReturn 124 }

      whenever(
        prisonerScheduledActivityRepository.getByScheduledInstanceIdAndPrisonerNumber(
          attendance.scheduledInstance.scheduledInstanceId,
          attendance.prisonerNumber,
        ),
      ).thenReturn(Optional.of(prisonerScheduledActivity))

      val allocation: Allocation = mock { on { payBand } doReturn thePayBand }

      whenever(allocationRepository.findById(124)).thenReturn(Optional.of(allocation))

      assertThat(service.determinePay(attendance)).isZero
    }

    @Test
    fun `returns zero if prisoner has no current incentive`() {
      val attendance = advanceAttendance()

      val activityPay = attendance.scheduledInstance.activitySchedule.activity.activityPay().first()

      val thePayBand = activityPay.payBand

      val prisoner: Prisoner = mock { on { currentIncentive } doReturn null }

      whenever(prisonerSearchApiClient.findByPrisonerNumber(attendance.prisonerNumber)).thenReturn(prisoner)

      val prisonerScheduledActivity: PrisonerScheduledActivity = mock { on { allocationId } doReturn 124 }

      whenever(
        prisonerScheduledActivityRepository.getByScheduledInstanceIdAndPrisonerNumber(
          attendance.scheduledInstance.scheduledInstanceId,
          attendance.prisonerNumber,
        ),
      ).thenReturn(Optional.of(prisonerScheduledActivity))

      val allocation: Allocation = mock { on { payBand } doReturn thePayBand }

      whenever(allocationRepository.findById(124)).thenReturn(Optional.of(allocation))

      assertThat(service.determinePay(attendance)).isZero
    }
  }
}
