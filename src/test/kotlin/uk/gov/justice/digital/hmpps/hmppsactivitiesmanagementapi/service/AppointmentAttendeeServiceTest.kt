package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.cancelOnTransferAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonerReleasedAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledOnTransferEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import java.util.Optional

@ExtendWith(FakeSecurityContext::class)
class AppointmentAttendeeServiceTest {
  private val appointmentAttendeeRemovalReasonRepository = mock<AppointmentAttendeeRemovalReasonRepository>()
  private val appointmentInstanceRepository = mock<AppointmentInstanceRepository>()
  private val appointmentAttendeeRepository = mock<AppointmentAttendeeRepository>()
  private val auditService = mock<AuditService>()

  private val appointmentAttendeeService = AppointmentAttendeeService(
    appointmentAttendeeRemovalReasonRepository,
    appointmentInstanceRepository,
    appointmentAttendeeRepository,
    TransactionHandler(),
    auditService,
  )

  @Test
  fun `removes prisoner from future appointments`() {
    val appointmentAttendeeId = 42L
    val prisonCode = "PVI"
    val prisonerNumber = "ABC123"
    val appointmentInstance = mock<AppointmentInstance>()
    val appointmentAttendee = mock<AppointmentAttendee>()

    whenever(appointmentInstance.appointmentAttendeeId).thenReturn(appointmentAttendeeId)
    whenever(appointmentInstance.prisonCode).thenReturn(prisonCode)
    whenever(appointmentInstance.prisonerNumber).thenReturn(prisonerNumber)

    whenever(appointmentAttendeeRemovalReasonRepository.findById(PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID))
      .thenReturn(Optional.of(prisonerReleasedAppointmentAttendeeRemovalReason()))

    whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber))
      .thenReturn(listOf(appointmentInstance))

    whenever(appointmentAttendeeRepository.findById(appointmentAttendeeId)).thenReturn(Optional.of(appointmentAttendee))

    appointmentAttendeeService.removePrisonerFromFutureAppointments(
      prisonCode,
      prisonerNumber,
      PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
      "OFFENDER_RELEASED_EVENT",
    )

    verify(appointmentAttendee).remove(
      any(),
      eq(prisonerReleasedAppointmentAttendeeRemovalReason()),
      eq("OFFENDER_RELEASED_EVENT"),
    )

    verify(auditService).logEvent(any<AppointmentCancelledOnTransferEvent>())
  }

  @Test
  fun `does not remove anything if there are no future appointments`() {
    val prisonCode = "PVI"
    val prisonerNumber = "ABC123"

    whenever(appointmentAttendeeRemovalReasonRepository.findById(CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID))
      .thenReturn(Optional.of(cancelOnTransferAppointmentAttendeeRemovalReason()))

    whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber))
      .thenReturn(listOf())

    appointmentAttendeeService.removePrisonerFromFutureAppointments(
      prisonCode,
      prisonerNumber,
      CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
      "APPOINTMENTS_CHANGED_EVENT",
    )

    verifyNoInteractions(appointmentAttendeeRepository)
    verifyNoInteractions(auditService)
  }
}
