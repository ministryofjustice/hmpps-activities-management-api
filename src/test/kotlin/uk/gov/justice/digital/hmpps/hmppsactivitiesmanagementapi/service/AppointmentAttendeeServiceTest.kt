package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.cancelOnTransferAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonerReleasedAppointmentAttendeeRemovalReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledOnTransferEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.AppointmentAttendeeRemovalReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentAttendeeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import java.time.LocalDateTime
import java.util.*

@ExtendWith(FakeSecurityContext::class)
class AppointmentAttendeeServiceTest {
  private val appointmentAttendeeRepository = mock<AppointmentAttendeeRepository>()
  private val appointmentInstanceRepository = mock<AppointmentInstanceRepository>()
  private val appointmentAttendeeRemovalReasonRepository = mock<AppointmentAttendeeRemovalReasonRepository>()
  private val outboundEventsService: OutboundEventsService = mock()
  private val auditService = mock<AuditService>()

  private val service = spy(
    AppointmentAttendeeService(
      appointmentAttendeeRepository,
      appointmentInstanceRepository,
      appointmentAttendeeRemovalReasonRepository,
      TransactionHandler(),
      outboundEventsService,
      auditService,
    ),
  )

  @Nested
  @DisplayName("Remove prisoner from future appointments")
  inner class RemovePrisonerFromFutureAppointments {
    @Test
    fun `removes prisoner from future appointments`() {
      val appointmentAttendeeId = 42L
      val prisonCode = "PVI"
      val prisonerNumber = "ABC123"
      val removedTime = LocalDateTime.now()
      val removedBy = "OFFENDER_RELEASED_EVENT"
      val appointmentInstance = mock<AppointmentInstance>()
      val appointmentAttendeeMock = mock<AppointmentAttendee>()
      val appointments = mock<Appointment>()

      whenever(appointmentInstance.appointmentAttendeeId).thenReturn(appointmentAttendeeId)
      whenever(appointmentInstance.prisonCode).thenReturn(prisonCode)
      whenever(appointmentInstance.prisonerNumber).thenReturn(prisonerNumber)
      whenever(appointmentAttendeeMock.appointment).thenReturn(appointments)
      whenever(appointments.categoryCode).thenReturn("VLB")

      whenever(appointmentAttendeeMock.appointmentAttendeeId).thenReturn(appointmentAttendeeId)
      whenever(
        appointmentAttendeeMock.remove(
          removedTime,
          prisonerReleasedAppointmentAttendeeRemovalReason(),
          removedBy,
        ),
      ).thenReturn(appointmentAttendeeMock)

      whenever(
        appointmentAttendeeRemovalReasonRepository.findById(
          PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        ),
      )
        .thenReturn(Optional.of(prisonerReleasedAppointmentAttendeeRemovalReason()))

      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(prisonCode, prisonerNumber))
        .thenReturn(listOf(appointmentInstance))

      whenever(appointmentAttendeeRepository.findById(appointmentInstance.appointmentAttendeeId))
        .thenReturn(Optional.of(appointmentAttendeeMock))

      service.removePrisonerFromFutureAppointments(
        prisonCode,
        prisonerNumber,
        removedTime,
        PRISONER_STATUS_RELEASED_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        removedBy,
      )

      verify(appointmentAttendeeMock).remove(
        removedTime,
        prisonerReleasedAppointmentAttendeeRemovalReason(),
        removedBy,
      )

      verify(auditService).logEvent(any<AppointmentCancelledOnTransferEvent>())
    }

    @Test
    fun `does not remove anything for video link court appointment`() {
      val appointmentInstance = mock<AppointmentInstance>()
      whenever(appointmentInstance.categoryCode) doReturn "VLB"

      whenever(
        appointmentAttendeeRemovalReasonRepository.findById(
          CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        ),
      ) doReturn Optional.of(cancelOnTransferAppointmentAttendeeRemovalReason())

      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(any(), any()))
        .thenReturn(listOf(appointmentInstance))

      service.removePrisonerFromFutureAppointments(
        "ABC",
        "123",
        LocalDateTime.now(),
        CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        "user",
      )

      verifyNoInteractions(appointmentAttendeeRepository)
      verifyNoInteractions(outboundEventsService)
      verifyNoInteractions(auditService)
    }

    @Test
    fun `does not remove anything for video link probation appointment`() {
      val appointmentInstance = mock<AppointmentInstance>()
      whenever(appointmentInstance.categoryCode) doReturn "VLPM"

      whenever(
        appointmentAttendeeRemovalReasonRepository.findById(
          CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        ),
      ) doReturn Optional.of(cancelOnTransferAppointmentAttendeeRemovalReason())

      whenever(appointmentInstanceRepository.findByPrisonCodeAndPrisonerNumberFromNow(any(), any()))
        .thenReturn(listOf(appointmentInstance))

      service.removePrisonerFromFutureAppointments(
        "ABC",
        "123",
        LocalDateTime.now(),
        CANCEL_ON_TRANSFER_APPOINTMENT_ATTENDEE_REMOVAL_REASON_ID,
        "user",
      )

      verifyNoInteractions(appointmentAttendeeRepository)
      verifyNoInteractions(outboundEventsService)
      verifyNoInteractions(auditService)
    }
  }
}
