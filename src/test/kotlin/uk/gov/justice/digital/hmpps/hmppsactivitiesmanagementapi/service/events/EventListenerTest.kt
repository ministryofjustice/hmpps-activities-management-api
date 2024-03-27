package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE

class EventListenerTest {

  private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule().apply {
    this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  private val inboundEventsService: InboundEventsService = mock()

  private val featureSwitches: FeatureSwitches = mock()

  private val eventListener = InboundEventsListener(objectMapper, inboundEventsService, featureSwitches)

  @Test
  fun `prisoner activities changed event - end`() {
    featureSwitches.stub { on { isEnabled(InboundEventType.ACTIVITIES_CHANGED) } doReturn true }

    eventListener.onMessage("/messages/prison-offender-events.prisoner.activities-changed.json".readRawMessage())

    verify(inboundEventsService).process(
      activitiesChangedEvent(
        prisonerNumber = "A5119DY",
        action = Action.END,
        prisonId = MOORLAND_PRISON_CODE,
      ),
    )
  }

  private fun String.readRawMessage() =
    EventListenerTest::class.java.getResource(this)?.readText() ?: throw AssertionError("cannot find file $this")
}
