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
      listOf(adjudicationHearing()).nomisAdjudicationsToScheduledEvents(moorlandPrisonCode, 99),
    ).containsExactly(
      ScheduledEvent(
        prisonCode = moorlandPrisonCode,
        eventSource = "NOMIS",
        eventId = null,
        bookingId = null,
        internalLocationId = -2,
        internalLocationCode = null,
        internalLocationDescription = "Adjudication room",
        appointmentInstanceId = null,
        appointmentOccurrenceId = null,
        scheduledInstanceId = null,
        oicHearingId = -1,
        comments = null,
        eventType = EventType.ADJUDICATION_HEARING.name,
        categoryCode = null,
        categoryDescription = "Governor's Hearing Adult",
        summary = "Governor's Hearing Adult",
        prisonerNumber = "1234567890",
        date = LocalDate.now(),
        startTime = LocalDate.now().atStartOfDay().toLocalTime(),
        endTime = LocalDate.now().atStartOfDay().toLocalTime().plusHours(ADJUDICATION_HEARING_DURATION_TWO_HOURS),
        priority = 99,
      ),
    )
  }
}
