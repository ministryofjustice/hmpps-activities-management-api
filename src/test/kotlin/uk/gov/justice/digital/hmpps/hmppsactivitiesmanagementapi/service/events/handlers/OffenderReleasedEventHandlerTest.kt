package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentAttendeeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.WaitingListService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ReleaseInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReleasedEvent
import java.time.LocalDate
import java.time.LocalDateTime

class OffenderReleasedEventHandlerTest {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock {
    on { findByCode(MOORLAND_PRISON_CODE) } doReturn
      rolloutPrison().copy(
        activitiesToBeRolledOut = true,
        activitiesRolloutDate = LocalDate.now().plusDays(-1),
      )
  }
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val appointmentAttendeeService: AppointmentAttendeeService = mock()
  private val waitingListService: WaitingListService = mock()
  private val prisonerAllocationHandler: PrisonerAllocationHandler = mock()
  private val allocationRepository: AllocationRepository = mock()

  private val handler = OffenderReleasedEventHandler(
    rolloutPrisonRepository,
    appointmentAttendeeService,
    waitingListService,
    prisonerSearchApiClient,
    prisonerAllocationHandler,
    allocationRepository,
  )

  private val inActiveOutPrisoner: Prisoner = mock { on { status } doReturn "INACTIVE OUT" }

  private val activeInPrisoner: Prisoner = mock { on { status } doReturn "ACTIVE IN" }

  private val releasedToHospitalPrisoner: Prisoner = mock { on { restrictedPatient } doReturn true }

  @BeforeEach
  fun beforeEach() {
    whenever(allocationRepository.existAtPrisonForPrisoner(any(), any(), eq(PrisonerStatus.allExcuding(PrisonerStatus.ENDED).toList()))) doReturn true
  }

  @Test
  fun `released event is not handled for an inactive prison`() {
    rolloutPrisonRepository.stub {
      on { findByCode(MOORLAND_PRISON_CODE) } doReturn
        rolloutPrison().copy(
          activitiesToBeRolledOut = false,
          activitiesRolloutDate = null,
        )
    }

    handler.handle(offenderReleasedEvent(MOORLAND_PRISON_CODE, "123456")).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(MOORLAND_PRISON_CODE)
    verifyNoInteractions(prisonerAllocationHandler)
  }

  @Test
  fun `release event is not processed when no matching prison is found`() {
    rolloutPrisonRepository.stub { on { findByCode(MOORLAND_PRISON_CODE) } doReturn null }

    handler.handle(offenderReleasedEvent(MOORLAND_PRISON_CODE, "123456")).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(MOORLAND_PRISON_CODE)
    verifyNoInteractions(prisonerAllocationHandler)
  }

  @Test
  fun `pending allocations are removed and un-ended allocations are ended on permanent release of inactive out prisoner`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn inActiveOutPrisoner

    handler.handle(offenderReleasedEvent(MOORLAND_PRISON_CODE, "123456")).also { it.isSuccess() isBool true }

    verify(waitingListService).removeOpenApplications(MOORLAND_PRISON_CODE, "123456", ServiceName.SERVICE_NAME.value)
    verify(prisonerAllocationHandler).deallocate(MOORLAND_PRISON_CODE, "123456", DeallocationReason.RELEASED)
  }

  @Test
  fun `pending allocations are removed and un-ended allocations are ended on permanent release of active in pentonville prisoner`() {
    activeInPrisoner.stub { on { prisonId } doReturn PENTONVILLE_PRISON_CODE }

    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn activeInPrisoner

    handler.handle(offenderReleasedEvent(MOORLAND_PRISON_CODE, "123456")).also { it.isSuccess() isBool true }

    verify(waitingListService).removeOpenApplications(MOORLAND_PRISON_CODE, "123456", ServiceName.SERVICE_NAME.value)
    verify(prisonerAllocationHandler).deallocate(MOORLAND_PRISON_CODE, "123456", DeallocationReason.RELEASED)
  }

  @Test
  fun `pending allocations are removed and un-ended allocations are ended on permanent release of restricted patient`() {
    activeInPrisoner.stub { on { prisonId } doReturn PENTONVILLE_PRISON_CODE }

    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn releasedToHospitalPrisoner

    handler.handle(offenderReleasedEvent(MOORLAND_PRISON_CODE, "123456", "RELEASED_TO_HOSPITAL")).also { it.isSuccess() isBool true }

    verify(waitingListService).removeOpenApplications(MOORLAND_PRISON_CODE, "123456", ServiceName.SERVICE_NAME.value)
    verify(prisonerAllocationHandler).deallocate(MOORLAND_PRISON_CODE, "123456", DeallocationReason.RELEASED)
  }

  @Test
  fun `permanent release of restricted patient removes them from future appointments`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn releasedToHospitalPrisoner

    handler.handle(offenderReleasedEvent(MOORLAND_PRISON_CODE, "123456", "RELEASED_TO_HOSPITAL")).also { it.isSuccess() isBool true }

    verify(appointmentAttendeeService).removePrisonerFromFutureAppointments(
      eq(MOORLAND_PRISON_CODE),
      eq("123456"),
      any<LocalDateTime>(),
      eq(PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID),
      eq("OFFENDER_RELEASED_EVENT"),
    )
  }

  @Test
  fun `permanent release of inactive out prisoner removes them from future appointments`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn inActiveOutPrisoner

    handler.handle(offenderReleasedEvent(MOORLAND_PRISON_CODE, "123456", "RELEASED_TO_HOSPITAL")).also { it.isSuccess() isBool true }

    verify(appointmentAttendeeService).removePrisonerFromFutureAppointments(
      eq(MOORLAND_PRISON_CODE),
      eq("123456"),
      any<LocalDateTime>(),
      eq(PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID),
      eq("OFFENDER_RELEASED_EVENT"),
    )
  }

  @Test
  fun `permanent release of active in prisoner removes them from future appointments`() {
    activeInPrisoner.stub { on { prisonId } doReturn PENTONVILLE_PRISON_CODE }

    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn activeInPrisoner

    handler.handle(offenderReleasedEvent(MOORLAND_PRISON_CODE, "123456")).also { it.isSuccess() isBool true }

    verify(appointmentAttendeeService).removePrisonerFromFutureAppointments(
      eq(MOORLAND_PRISON_CODE),
      eq("123456"),
      any<LocalDateTime>(),
      eq(PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID),
      eq("OFFENDER_RELEASED_EVENT"),
    )
  }

  @Test
  fun `throws null pointer exception when cannot find prisoners details`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn null

    assertThatThrownBy {
      handler.handle(offenderReleasedEvent(MOORLAND_PRISON_CODE, "123456")).also { it.isSuccess() isBool true }
    }.isInstanceOf(NullPointerException::class.java)
      .hasMessage("Prisoner search lookup failed for prisoner 123456")
  }

  @Test
  fun `allocation is unmodified for unknown release event`() {
    val outcome = handler.handle(
      OffenderReleasedEvent(
        ReleaseInformation(
          nomsNumber = "12345",
          reason = "UNKNOWN",
          prisonId = MOORLAND_PRISON_CODE,
        ),
      ),
    )

    assertThat(outcome.isSuccess()).isFalse
  }

  @Test
  fun `no interactions when released prisoner has no allocations of interest`() {
    whenever(allocationRepository.existAtPrisonForPrisoner(any(), any(), eq(PrisonerStatus.allExcuding(PrisonerStatus.ENDED).toList()))) doReturn false

    handler.handle(offenderReleasedEvent(MOORLAND_PRISON_CODE, "123456")).also { it.isSuccess() isBool true }

    verifyNoInteractions(prisonerSearchApiClient)
    verifyNoInteractions(prisonerAllocationHandler)
  }
}
