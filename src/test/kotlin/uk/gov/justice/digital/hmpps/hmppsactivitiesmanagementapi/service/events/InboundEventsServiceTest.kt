package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReceivedEventHandler
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers.OffenderReleasedEventHandler

class InboundEventsServiceTest {
  private val inboundEvent = offenderReleasedEvent(moorlandPrisonCode, "123456")
  private val repository: RolloutPrisonRepository = mock()
  private val receivedEventHandler: OffenderReceivedEventHandler = mock()
  private val releasedEventHandler: OffenderReleasedEventHandler = mock()
  private val service = InboundEventsService(repository, releasedEventHandler, receivedEventHandler)

  @Test
  fun `inbound event is processed for active prison`() {
    repository.stub { on { findByCode(moorlandPrisonCode) } doReturn rolloutPrison() }

    service.process(inboundEvent)

    verify(repository).findByCode(moorlandPrisonCode)
    verify(releasedEventHandler).handle(inboundEvent)
  }

  @Test
  fun `inbound event is not processed for inactive prison`() {
    repository.stub {
      on { findByCode(moorlandPrisonCode) } doReturn
        rolloutPrison().copy(
          activitiesToBeRolledOut = false,
          activitiesRolloutDate = null,
        )
    }

    service.process(inboundEvent)

    verify(repository).findByCode(moorlandPrisonCode)
    verifyNoInteractions(releasedEventHandler)
  }

  @Test
  fun `inbound event is not processed when no matching prison`() {
    repository.stub { on { findByCode(moorlandPrisonCode) } doReturn null }

    service.process(inboundEvent)

    verify(repository).findByCode(moorlandPrisonCode)
    verifyNoInteractions(releasedEventHandler)
  }
}
