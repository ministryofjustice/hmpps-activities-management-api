package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerScheduledEvents
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformToPrisonerScheduledEvents

@Service
class ScheduledEventService(private val prisonApiClient: PrisonApiClient) {

  fun getScheduledEventsByDateRange(
    prisonCode: String,
    prisonerNumber: String,
    dateRange: LocalDateRange
  ): PrisonerScheduledEvents? {

    val prisonerDetail = prisonApiClient.getPrisonerDetails(prisonerNumber).block()
    if (prisonerDetail === null || prisonerDetail.agencyId != prisonCode || prisonerDetail.bookingId === null) {
      return null
    }
    val prisonApiCalls = Mono.zip(
      prisonApiClient.getScheduledAppointments(prisonerDetail.bookingId, dateRange),
      prisonApiClient.getScheduledCourtHearings(prisonerDetail.bookingId, dateRange)
    )
      .map { t ->
        transformToPrisonerScheduledEvents(
          prisonCode,
          prisonerNumber,
          dateRange,
          t.t1,
          t.t2,
        )
      }

    return prisonApiCalls.block()
  }
}
