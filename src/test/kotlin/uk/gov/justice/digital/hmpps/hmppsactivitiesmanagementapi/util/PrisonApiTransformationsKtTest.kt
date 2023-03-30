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
      listOf(adjudicationHearing()).prisonApiPrisonOffenderAdjudicationsToScheduledEvents(moorlandPrisonCode, 99),
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
        eventTypeDesc = "Governor's Hearing Adult",
        event = null,
        eventDesc = null,
        details = null,
        prisonerNumber = "1234567890",
        date = LocalDate.now(),
        startTime = LocalDate.now().atStartOfDay().toLocalTime(),
        endTime = LocalDate.now().atStartOfDay().toLocalTime().plusHours(ADJUDICATION_HEARING_DURATION_TWO_HOURS),
        priority = 99,
      ),
    )
  }
}
