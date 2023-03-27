package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.adjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import java.time.LocalDate

class PrisonApiTransformationsKtTest {

  @Test
  fun `transform offender adjudications to scheduled events`() {
    assertThat(
      listOf(adjudicationHearing()).prisonApiOffenderAdjudicationsToScheduledEvents(moorlandPrisonCode, emptyList()),
    ).containsExactly(
      ScheduledEvent(
        prisonCode = moorlandPrisonCode,
        eventId = -1,
        bookingId = null,
        locationId = -2,
        location = "Adjudication room",
        eventClass = null,
        eventStatus = "SCH",
        eventType = EventType.ADJUDICATION_HEARING.name,
        eventTypeDesc = null,
        event = null,
        eventDesc = null,
        details = null,
        prisonerNumber = "1234567890",
        date = LocalDate.now(),
        startTime = LocalDate.now().atStartOfDay().toLocalTime(),
        endTime = null, // TODO consider defaulting a fixed number of hours as adjudications have no concept of end time.
        priority = EventType.ADJUDICATION_HEARING.defaultPriority,
      ),
    )
  }
}
