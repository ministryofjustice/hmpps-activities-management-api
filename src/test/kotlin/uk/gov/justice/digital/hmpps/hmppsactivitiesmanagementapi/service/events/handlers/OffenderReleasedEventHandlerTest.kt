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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentAttendeeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ReleaseInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReleasedEvent
import java.time.LocalDate

class OffenderReleasedEventHandlerTest {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock {
    on { findByCode(moorlandPrisonCode) } doReturn
      rolloutPrison().copy(
        activitiesToBeRolledOut = true,
        activitiesRolloutDate = LocalDate.now().plusDays(-1),
      )
  }
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val appointmentAttendeeService: AppointmentAttendeeService = mock()
  private val prisonerAllocationHandler: PrisonerAllocationHandler = mock()
  private val allocationRepository: AllocationRepository = mock()

  private val handler = OffenderReleasedEventHandler(
    rolloutPrisonRepository,
    appointmentAttendeeService,
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
      on { findByCode(moorlandPrisonCode) } doReturn
        rolloutPrison().copy(
          activitiesToBeRolledOut = false,
          activitiesRolloutDate = null,
        )
    }

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456")).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(moorlandPrisonCode)
    verifyNoInteractions(prisonerAllocationHandler)
  }

  @Test
  fun `release event is not processed when no matching prison is found`() {
    rolloutPrisonRepository.stub { on { findByCode(moorlandPrisonCode) } doReturn null }

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456")).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(moorlandPrisonCode)
    verifyNoInteractions(prisonerAllocationHandler)
  }

  @Test
  fun `pending allocations are removed and un-ended allocations are ended on permanent release of inactive out prisoner`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn inActiveOutPrisoner

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456")).also { it.isSuccess() isBool true }

    verify(prisonerAllocationHandler).deallocate(moorlandPrisonCode, "123456", DeallocationReason.RELEASED)
  }

  @Test
  fun `pending allocations are removed and un-ended allocations are ended on permanent release of active in pentonville prisoner`() {
    activeInPrisoner.stub { on { prisonId } doReturn pentonvillePrisonCode }

    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn activeInPrisoner

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456")).also { it.isSuccess() isBool true }

    verify(prisonerAllocationHandler).deallocate(moorlandPrisonCode, "123456", DeallocationReason.RELEASED)
  }

  @Test
  fun `pending allocations are removed and un-ended allocations are ended on permanent release of restricted patient`() {
    activeInPrisoner.stub { on { prisonId } doReturn pentonvillePrisonCode }

    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn releasedToHospitalPrisoner

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456", "RELEASED_TO_HOSPITAL")).also { it.isSuccess() isBool true }

    verify(prisonerAllocationHandler).deallocate(moorlandPrisonCode, "123456", DeallocationReason.RELEASED)
  }

  @Test
  fun `future appointments are ended on permanent release of restricted patient`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn releasedToHospitalPrisoner

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456", "RELEASED_TO_HOSPITAL")).also { it.isSuccess() isBool true }

    verify(appointmentAttendeeService).cancelFutureOffenderAppointments(moorlandPrisonCode, "123456")
  }

  @Test
  fun `future appointments are ended on permanent release of inactive out prisoner`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn inActiveOutPrisoner

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456", "RELEASED_TO_HOSPITAL")).also { it.isSuccess() isBool true }

    verify(appointmentAttendeeService).cancelFutureOffenderAppointments(moorlandPrisonCode, "123456")
  }

  @Test
  fun `future appointments are ended on permanent release of active in pentonville prisoner`() {
    activeInPrisoner.stub { on { prisonId } doReturn pentonvillePrisonCode }

    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn activeInPrisoner

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456")).also { it.isSuccess() isBool true }

    verify(appointmentAttendeeService).cancelFutureOffenderAppointments(moorlandPrisonCode, "123456")
  }

  @Test
  fun `throws nullpointer exception when cannot find prisoners details`() {
    whenever(prisonerSearchApiClient.findByPrisonerNumber("123456")) doReturn null

    assertThatThrownBy {
      handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456")).also { it.isSuccess() isBool true }
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
          prisonId = moorlandPrisonCode,
        ),
      ),
    )

    assertThat(outcome.isSuccess()).isFalse
  }

  @Test
  fun `no interactions when released prisoner has no allocations of interest`() {
    whenever(allocationRepository.existAtPrisonForPrisoner(any(), any(), eq(PrisonerStatus.allExcuding(PrisonerStatus.ENDED).toList()))) doReturn false

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456")).also { it.isSuccess() isBool true }

    verifyNoInteractions(prisonerSearchApiClient)
    verifyNoInteractions(prisonerAllocationHandler)
  }
}
