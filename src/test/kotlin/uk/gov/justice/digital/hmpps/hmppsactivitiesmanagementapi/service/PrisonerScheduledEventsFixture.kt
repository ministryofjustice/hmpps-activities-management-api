package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import java.time.LocalDate

class PrisonerScheduledEventsFixture {
  companion object {
    fun instance(
      prisonCode: String = "MDI",
      prisonerNumber: String = "GF10001",
      startDate: LocalDate = LocalDate.of(2022, 10, 1),
      endDate: LocalDate = LocalDate.of(2022, 11, 29),
      appointments: List<ScheduledEvent> = listOf(ScheduledEventFixture.instance()),
      courtHearings: List<ScheduledEvent> = listOf(ScheduledEventFixture.instance()),
    ): PrisonerScheduledEvents = PrisonerScheduledEvents(
      prisonCode = prisonCode,
      prisonerNumber = prisonerNumber,
      startDate = startDate,
      endDate = endDate,
      appointments = appointments,
      courtHearings = courtHearings
    )
  }
}
