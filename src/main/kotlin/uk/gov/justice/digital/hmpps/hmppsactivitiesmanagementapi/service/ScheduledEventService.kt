package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformToScheduledEvents

@Service
class ScheduledEventService(private val prisonApiClient: PrisonApiClient) {

  fun getScheduledEventsByDateRange(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange
  ): List<ScheduledEvent> {

    val prisonerDetail = prisonApiClient.getPrisonerDetails(prisonerNumber).block()
    if (prisonerDetail === null || prisonerDetail.agencyId != prisonCode || prisonerDetail.bookingId === null) {
      return emptyList()
    }
    val appointments = prisonApiClient.getScheduledAppointments(prisonerDetail.bookingId, dateRange).block()

    if (appointments.isNullOrEmpty()) return emptyList() else
      return transformToScheduledEvents(appointments, prisonerNumber)
  }
}
