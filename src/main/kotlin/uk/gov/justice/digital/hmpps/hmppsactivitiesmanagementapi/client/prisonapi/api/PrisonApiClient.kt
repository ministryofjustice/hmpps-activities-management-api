package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

@Service
class PrisonApiClient(private val prisonApiWebClient: WebClient) {

  fun getPrisonerDetails(prisonerNumber: String): Mono<InmateDetail> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/offenderNo/{prisonerNumber}")
          .queryParam("fullInfo", true)
          .build(prisonerNumber)
      }
      .retrieve()
      .bodyToMono(typeReference<InmateDetail>())
  }

  fun getScheduledAppointments(bookingId: Long, dateRange: LocalDateRange): Mono<List<PrisonApiScheduledEvent>> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/{bookingId}/appointments")
          .queryParam("startDate", dateRange.start)
          .queryParam("endDate", dateRange.endInclusive)
          .build(bookingId)
      }
      .retrieve()
      .bodyToMono(typeReference<List<PrisonApiScheduledEvent>>())
  }

  fun getScheduledCourtHearings(bookingId: Long, dateRange: LocalDateRange): Mono<CourtHearings> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/{bookingId}/court-hearings")
          .queryParam("startDate", dateRange.start)
          .queryParam("endDate", dateRange.endInclusive)
          .build(bookingId)
      }
      .retrieve()
      .bodyToMono(typeReference<CourtHearings>())
  }

  fun getScheduledVisits(bookingId: Long, dateRange: LocalDateRange): Mono<List<PrisonApiScheduledEvent>> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/{bookingId}/visits")
          .queryParam("startDate", dateRange.start)
          .queryParam("endDate", dateRange.endInclusive)
          .build(bookingId)
      }
      .retrieve()
      .bodyToMono(typeReference<List<PrisonApiScheduledEvent>>())
  }
}
