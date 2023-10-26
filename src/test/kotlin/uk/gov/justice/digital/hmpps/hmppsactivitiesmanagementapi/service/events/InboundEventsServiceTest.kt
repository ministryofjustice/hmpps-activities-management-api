package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.ActivitiesChangedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.AppointmentChangedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.InterestingEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReceivedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReleasedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.Outcome

class InboundEventsServiceTest {
  private val receivedEventHandler: OffenderReceivedEventHandler = mock()
  private val releasedEventHandler: OffenderReleasedEventHandler = mock()
  private val interestingEventHandler: InterestingEventHandler = mock()
  private val activitiesChangedEventHandler: ActivitiesChangedEventHandler = mock()
  private val appointmentsChangedEventHandler: AppointmentChangedEventHandler = mock()

  private val service = InboundEventsService(
    releasedEventHandler,
    receivedEventHandler,
    interestingEventHandler,
    activitiesChangedEventHandler,
    appointmentsChangedEventHandler,
  )

  @BeforeEach
  fun setupMocks() {
    reset(receivedEventHandler, releasedEventHandler, interestingEventHandler)
    whenever(releasedEventHandler.handle(any())).thenReturn(Outcome.success())
    whenever(receivedEventHandler.handle(any())).thenReturn(Outcome.success())
    whenever(interestingEventHandler.handle(any())).thenReturn(Outcome.success())
  }

  @Test
  fun `inbound released event is processed by release event handler`() {
    val offenderReleasedEvent = offenderReleasedEvent(moorlandPrisonCode, "123456")
    service.process(offenderReleasedEvent)
    verify(releasedEventHandler).handle(offenderReleasedEvent)
  }

  @Test
  fun `inbound released event is handled as an interesting event`() {
    val offenderReleasedEvent = offenderReleasedEvent(moorlandPrisonCode, "123456")
    service.process(offenderReleasedEvent)
    verify(interestingEventHandler).handle(offenderReleasedEvent)
  }

  @Test
  fun `inbound activities changed event is processed by release event handler`() {
    val activitiesChangedEvent = activitiesChangedEvent(prisonId = moorlandPrisonCode, prisonerNumber = "123456", action = Action.END)
    service.process(activitiesChangedEvent)
    verify(activitiesChangedEventHandler).handle(activitiesChangedEvent)
  }

  @Test
  fun `inbound activities changed event is handled as an interesting event`() {
    val activitiesChangedEvent = activitiesChangedEvent(prisonId = moorlandPrisonCode, prisonerNumber = "123456", action = Action.END)
    service.process(activitiesChangedEvent)
    verify(interestingEventHandler).handle(activitiesChangedEvent)
  }

  @Test
  fun `inbound received event is processed by received event handler`() {
    val offenderReceivedEvent = offenderReceivedFromTemporaryAbsence(moorlandPrisonCode, "123456")
    service.process(offenderReceivedEvent)
    verify(receivedEventHandler).handle(offenderReceivedEvent)
  }

  @Test
  fun `inbound received event failure is handled as an interesting event`() {
    val offenderReceivedEvent = offenderReceivedFromTemporaryAbsence(moorlandPrisonCode, "123456").also {
      whenever(receivedEventHandler.handle(it)).thenReturn(Outcome.failed())
    }
    service.process(offenderReceivedEvent)
    verify(receivedEventHandler).handle(offenderReceivedEvent)
    verify(interestingEventHandler).handle(offenderReceivedEvent)
  }

  @Test
  fun `inbound interesting event is processed by interesting event handler`() {
    val cellMoveEvent = cellMoveEvent("123456")
    service.process(cellMoveEvent)
    verify(interestingEventHandler).handle(cellMoveEvent)
  }

  @Test
  fun `inbound appointments changed event is processed by appointments changed event handler`() {
    val appointmentsChangedEvent = appointmentsChangedEvent("123456")
    service.process(appointmentsChangedEvent)
    verify(appointmentsChangedEventHandler).handle(appointmentsChangedEvent)
  }

  @Test
  fun `inbound appointments changed event is processed by  interesting event handler`() {
    val appointmentsChangedEvent = appointmentsChangedEvent("123456")
    service.process(appointmentsChangedEvent)
    verify(interestingEventHandler).handle(appointmentsChangedEvent)
  }
}
