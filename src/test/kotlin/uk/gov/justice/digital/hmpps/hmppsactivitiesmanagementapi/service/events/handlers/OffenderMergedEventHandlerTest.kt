package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.InmateDetailFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderMergedEvent

class OffenderMergedEventHandlerTest {
  private val prisonerApiClient: PrisonApiApplicationClient = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val attendanceRepository: AttendanceRepository = mock()
  private val waitingListRepository: WaitingListRepository = mock()
  private val auditRepository: AuditRepository = mock()
  private val eventReviewRepository: EventReviewRepository = mock()
  private val appointmentAttendeeRepository: AppointmentAttendeeRepository = mock()
  private val featureSwitches: FeatureSwitches = mock { on { isEnabled(Feature.OFFENDER_MERGED_ENABLED) } doReturn true }

  // Define old and new prisoner numbers in the merge
  private val oldNumber = "A1111AA"
  private val newNumber = "B2222BB"
  private val newBookingId = 999999L

  private val prisonerSearchResult = InmateDetailFixture.instance(
    agencyId = MOORLAND_PRISON_CODE,
    offenderNo = newNumber,
    firstName = "Stephen",
    lastName = "Macdonald",
    bookingId = newBookingId,
  )

  private val handler = OffenderMergedEventHandler(
    prisonerApiClient,
    allocationRepository,
    attendanceRepository,
    waitingListRepository,
    auditRepository,
    eventReviewRepository,
    appointmentAttendeeRepository,
    TransactionHandler(),
    featureSwitches,
  )

  @BeforeEach
  fun beforeTests() {
    prisonerApiClient.stub {
      on { getPrisonerDetailsLite(newNumber) } doReturn prisonerSearchResult
    }

    val allocation: Allocation = mock()

    allocationRepository.stub {
      on { findByPrisonCodeAndPrisonerNumber(MOORLAND_PRISON_CODE, newNumber) } doReturn listOf(allocation)
    }
  }

  @Test
  fun `inbound merged event is processed when the prisoner is at a rolled out prison`() {
    val inboundEvent = offenderMergedEvent(prisonerNumber = newNumber, removedPrisonerNumber = oldNumber)

    handler.handle(inboundEvent).also { it.isSuccess() isBool true }

    verify(prisonerApiClient).getPrisonerDetailsLite(newNumber)

    verify(allocationRepository).findByPrisonCodeAndPrisonerNumber(MOORLAND_PRISON_CODE, newNumber)
    verify(allocationRepository).findByPrisonCodeAndPrisonerNumber(MOORLAND_PRISON_CODE, oldNumber)

    inOrder(allocationRepository) {
      verify(allocationRepository).mergePrisonerToNewBookingId(newNumber, newBookingId)
      verify(allocationRepository).mergeOldPrisonerNumberToNew(oldNumber, newNumber, newBookingId)
    }

    verify(attendanceRepository).findByPrisonerNumber(oldNumber)
    verify(attendanceRepository).mergeOldPrisonerNumberToNew(oldNumber, newNumber)

    verify(waitingListRepository).findByPrisonCodeAndPrisonerNumber(MOORLAND_PRISON_CODE, newNumber)
    verify(waitingListRepository).findByPrisonCodeAndPrisonerNumber(MOORLAND_PRISON_CODE, oldNumber)

    inOrder(waitingListRepository) {
      verify(waitingListRepository).mergePrisonerToNewBookingId(newNumber, newBookingId)
      verify(waitingListRepository).mergeOldPrisonerNumberToNew(oldNumber, newNumber, newBookingId)
    }

    verify(auditRepository).findByPrisonCodeAndPrisonerNumber(MOORLAND_PRISON_CODE, oldNumber)
    verify(auditRepository).mergeOldPrisonerNumberToNew(oldNumber, newNumber)

    verify(eventReviewRepository).findByPrisonCodeAndPrisonerNumber(MOORLAND_PRISON_CODE, newNumber)
    verify(eventReviewRepository).findByPrisonCodeAndPrisonerNumber(MOORLAND_PRISON_CODE, oldNumber)

    inOrder(eventReviewRepository) {
      verify(eventReviewRepository).mergePrisonerToNewBookingId(newNumber, newBookingId)
      verify(eventReviewRepository).mergeOldPrisonerNumberToNew(oldNumber, newNumber, newBookingId)
    }

    verify(appointmentAttendeeRepository).findByPrisonerNumber(newNumber)
    verify(appointmentAttendeeRepository).findByPrisonerNumber(oldNumber)

    inOrder(appointmentAttendeeRepository) {
      verify(appointmentAttendeeRepository).mergePrisonerToNewBookingId(newNumber, newBookingId)
      verify(appointmentAttendeeRepository).mergeOldPrisonerNumberToNew(oldNumber, newNumber, newBookingId)
    }

    verify(auditRepository).findByPrisonCodeAndPrisonerNumber(MOORLAND_PRISON_CODE, oldNumber)
    verify(auditRepository).mergeOldPrisonerNumberToNew(oldNumber, newNumber)
    verify(auditRepository).save(any())
  }
}
