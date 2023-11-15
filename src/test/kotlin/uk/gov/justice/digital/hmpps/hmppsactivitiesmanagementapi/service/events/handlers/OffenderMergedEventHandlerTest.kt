package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderMergedEvent
import java.time.LocalDate

class OffenderMergedEventHandlerTest {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiApplicationClient = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val attendanceRepository: AttendanceRepository = mock()
  private val waitingListRepository: WaitingListRepository = mock()
  private val auditRepository: AuditRepository = mock()
  private val eventReviewRepository: EventReviewRepository = mock()
  private val appointmentAttendeeRepository: AppointmentAttendeeRepository = mock()

  // Define old and new prisoner numbers in the merge
  private val oldNumber = "A1111AA"
  private val newNumber = "B2222BB"

  private val prisonerSearchResult: Prisoner = mock {
    on { prisonerNumber } doReturn newNumber
    on { firstName } doReturn "Stephen"
    on { lastName } doReturn "Macdonald"
    on { status } doReturn "ACTIVE IN"
    on { prisonId } doReturn moorlandPrisonCode
  }

  private val handler = OffenderMergedEventHandler(
    rolloutPrisonRepository,
    prisonerSearchApiClient,
    allocationRepository,
    attendanceRepository,
    waitingListRepository,
    auditRepository,
    eventReviewRepository,
    appointmentAttendeeRepository,
    TransactionHandler(),
  )

  @BeforeEach
  fun beforeTests() {
    reset(
      rolloutPrisonRepository,
      prisonerSearchApiClient,
      allocationRepository,
      attendanceRepository,
      waitingListRepository,
      auditRepository,
      eventReviewRepository,
      appointmentAttendeeRepository,
    )

    rolloutPrisonRepository.stub {
      on { findByCode(moorlandPrisonCode) } doReturn
        rolloutPrison().copy(
          activitiesToBeRolledOut = true,
          activitiesRolloutDate = LocalDate.now().plusDays(-1),
        )
    }

    prisonerSearchApiClient.stub {
      on { findByPrisonerNumber(newNumber) } doReturn prisonerSearchResult
    }
  }

  @Test
  fun `inbound merged event is processed when the prisoner is at a rolled out prison`() {
    val inboundEvent = offenderMergedEvent(prisonerNumber = newNumber, removedPrisonerNumber = oldNumber)

    val outcome = handler.handle(inboundEvent)

    assertThat(outcome.isSuccess()).isTrue

    verify(prisonerSearchApiClient).findByPrisonerNumber(newNumber)
    verify(rolloutPrisonRepository).findByCode(moorlandPrisonCode)
    verify(allocationRepository).findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, oldNumber)
    verify(attendanceRepository).findByPrisonerNumber(oldNumber)
    verify(waitingListRepository).findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, oldNumber)
    verify(auditRepository).findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, oldNumber)
    verify(eventReviewRepository).findByPrisonCodeAndPrisonerNumber(moorlandPrisonCode, oldNumber)
    verify(appointmentAttendeeRepository).findByPrisonerNumber(oldNumber)
    verify(auditRepository).save(any())
  }

  @Test
  fun `inbound merged event is ignored when the prisoner is not at a rolled out prison`() {
    val inboundEvent = offenderMergedEvent(prisonerNumber = newNumber, removedPrisonerNumber = oldNumber)

    rolloutPrisonRepository.stub {
      on { findByCode(moorlandPrisonCode) } doReturn
        rolloutPrison().copy(
          activitiesToBeRolledOut = false,
          activitiesRolloutDate = null,
        )
    }

    val outcome = handler.handle(inboundEvent)

    assertThat(outcome.isSuccess()).isTrue

    verify(prisonerSearchApiClient).findByPrisonerNumber(newNumber)
    verify(rolloutPrisonRepository).findByCode(moorlandPrisonCode)
    verifyNoInteractions(
      allocationRepository,
      attendanceRepository,
      waitingListRepository,
      auditRepository,
      eventReviewRepository,
      appointmentAttendeeRepository,
    )
  }
}
