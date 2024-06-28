package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.adjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.locations
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.visit
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import java.time.LocalDate

class PrisonApiTransformationsTest {

  @Test
  fun `transform offender adjudications to scheduled events with user description`() {
    assertThat(
      listOf(adjudicationHearing()).nomisAdjudicationsToScheduledEvents(
        MOORLAND_PRISON_CODE,
        99,
        locations(
          locationId = -2,
          userDescription = "Adjudication user description",
          description = "Adjudication description",
        ),
      ),
    ).containsExactly(
      ScheduledEvent(
        prisonCode = MOORLAND_PRISON_CODE,
        eventSource = "NOMIS",
        eventId = null,
        bookingId = null,
        internalLocationId = -2,
        internalLocationCode = null,
        internalLocationUserDescription = "Adjudication user description",
        internalLocationDescription = "Adjudication description",
        appointmentSeriesId = null,
        appointmentId = null,
        appointmentAttendeeId = null,
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
        appointmentSeriesCancellationStartDate = null,
        appointmentSeriesCancellationStartTime = null,
        appointmentSeriesFrequency = null,
      ),
    )
  }

  @Test
  fun `transform offender adjudications to scheduled events without user description`() {
    assertThat(
      listOf(adjudicationHearing()).nomisAdjudicationsToScheduledEvents(MOORLAND_PRISON_CODE, 99, emptyMap()),
    ).containsExactly(
      ScheduledEvent(
        prisonCode = MOORLAND_PRISON_CODE,
        eventSource = "NOMIS",
        eventId = null,
        bookingId = null,
        internalLocationId = -2,
        internalLocationCode = null,
        internalLocationUserDescription = null,
        internalLocationDescription = "Adjudication room",
        appointmentSeriesId = null,
        appointmentId = null,
        appointmentAttendeeId = null,
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
        appointmentSeriesCancellationStartDate = null,
        appointmentSeriesCancellationStartTime = null,
        appointmentSeriesFrequency = null,
      ),
    )
  }

  @Test
  fun `transform visit to scheduled event`() {
    val now = TimeSource.now()

    assertThat(
      listOf(visit(dateTime = now)).multiplePrisonerVisitsToScheduledEvents(prisonCode = MOORLAND_PRISON_CODE, 1),
    ).containsExactly(
      ScheduledEvent(
        prisonCode = MOORLAND_PRISON_CODE,
        eventSource = "NOMIS",
        eventId = null,
        bookingId = null,
        internalLocationId = -1,
        internalLocationCode = null,
        internalLocationUserDescription = null,
        internalLocationDescription = "visit event location",
        appointmentSeriesId = null,
        appointmentId = null,
        appointmentAttendeeId = null,
        scheduledInstanceId = null,
        oicHearingId = null,
        comments = "visit comments",
        eventType = EventType.VISIT.name,
        categoryCode = "event code",
        categoryDescription = null,
        summary = "visit event description",
        prisonerNumber = "G4793VF",
        date = now.toLocalDate(),
        startTime = now.toLocalTime(),
        endTime = null,
        priority = 1,
        appointmentSeriesCancellationStartDate = null,
        appointmentSeriesCancellationStartTime = null,
        appointmentSeriesFrequency = null,
      ),
    )
  }
}
