package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import java.time.LocalDate

object PrisonerScheduledEventsFixture {
  fun instance(
    prisonCode: String = "MDI",
    prisonerNumbers: Set<String> = setOf("GF10001"),
    startDate: LocalDate = LocalDate.of(2022, 10, 1),
    endDate: LocalDate = LocalDate.of(2022, 11, 29),
    appointments: List<ScheduledEvent> = listOf(ScheduledEventFixture.appointmentInstance()),
    activities: List<ScheduledEvent> = listOf(ScheduledEventFixture.activityInstance()),
    visits: List<ScheduledEvent> = listOf(ScheduledEventFixture.visitInstance()),
    courtHearings: List<ScheduledEvent> = listOf(ScheduledEventFixture.courtHearingInstance()),
  ) = PrisonerScheduledEvents(
    prisonCode = prisonCode,
    prisonerNumbers = prisonerNumbers,
    startDate = startDate,
    endDate = endDate,
    appointments = appointments,
    activities = activities,
    visits = visits,
    courtHearings = courtHearings,
  )
}
