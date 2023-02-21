package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import java.time.LocalDate
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

@Service
class PrisonApiClient(private val prisonApiWebClient: WebClient) {

  fun getPrisonerDetails(prisonerNumber: String, fullInfo: Boolean = true): Mono<InmateDetail> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/offenderNo/{prisonerNumber}")
          .queryParam("fullInfo", fullInfo)
          .build(prisonerNumber)
      }
      .retrieve()
      .bodyToMono(typeReference<InmateDetail>())
  }

  fun getScheduledActivities(bookingId: Long, dateRange: LocalDateRange): Mono<List<PrisonApiScheduledEvent>> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/{bookingId}/activities")
          .queryParam("fromDate", dateRange.start)
          .queryParam("toDate", dateRange.endInclusive)
          .build(bookingId)
      }
      .retrieve()
      .bodyToMono(typeReference<List<PrisonApiScheduledEvent>>())
  }

  fun getScheduledAppointments(bookingId: Long, dateRange: LocalDateRange): Mono<List<PrisonApiScheduledEvent>> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/{bookingId}/appointments")
          .queryParam("fromDate", dateRange.start)
          .queryParam("toDate", dateRange.endInclusive)
          .build(bookingId)
      }
      .retrieve()
      .bodyToMono(typeReference<List<PrisonApiScheduledEvent>>())
  }

  fun getScheduledAppointmentsForPrisonerNumbers(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?
  ): Mono<List<PrisonerSchedule>> {
    return prisonApiWebClient.post()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/schedules/{prisonCode}/appointments")
          .maybeQueryParam("date", date)
          .maybeQueryParam("timeSlot", timeSlot)
          .build(prisonCode)
      }
      .bodyValue(prisonerNumbers)
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerSchedule>>())
  }

  fun getScheduledCourtHearings(bookingId: Long, dateRange: LocalDateRange): Mono<CourtHearings> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/{bookingId}/court-hearings")
          .queryParam("fromDate", dateRange.start)
          .queryParam("toDate", dateRange.endInclusive)
          .build(bookingId)
      }
      .retrieve()
      .bodyToMono(typeReference<CourtHearings>())
  }

  fun getScheduledCourtEventsForPrisonerNumbers(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?
  ): Mono<List<PrisonerSchedule>> {
    return prisonApiWebClient.post()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/schedules/{prisonCode}/courtEvents")
          .maybeQueryParam("date", date)
          .maybeQueryParam("timeSlot", timeSlot)
          .build(prisonCode)
      }
      .bodyValue(prisonerNumbers)
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerSchedule>>())
  }

  fun getScheduledVisits(bookingId: Long, dateRange: LocalDateRange): Mono<List<PrisonApiScheduledEvent>> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/{bookingId}/visits")
          .queryParam("fromDate", dateRange.start)
          .queryParam("toDate", dateRange.endInclusive)
          .build(bookingId)
      }
      .retrieve()
      .bodyToMono(typeReference<List<PrisonApiScheduledEvent>>())
  }

  fun getScheduledVisitsForPrisonerNumbers(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?
  ): Mono<List<PrisonerSchedule>> {
    return prisonApiWebClient.post()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/schedules/{prisonCode}/visits")
          .maybeQueryParam("date", date)
          .maybeQueryParam("timeSlot", timeSlot)
          .build(prisonCode)
      }
      .bodyValue(prisonerNumbers)
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerSchedule>>())
  }

  fun getScheduledActivitiesForPrisonerNumbers(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?
  ): Mono<List<PrisonerSchedule>> {
    return prisonApiWebClient.post()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/schedules/{prisonCode}/activities")
          .maybeQueryParam("date", date)
          .maybeQueryParam("timeSlot", timeSlot)
          .build(prisonCode)
      }
      .bodyValue(prisonerNumbers)
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerSchedule>>())
  }

  /*
  Will possibly re-introduce this method if we ever need to get ALL activities in a prison from NOMIS.
  At present, we only get these for either a prisoner, or a list of prisoners.

  fun getScheduledActivitiesForDateRange(
    prisonCode: String,
    dateRange: LocalDateRange,
  ): Mono<List<PrisonerSchedule>> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/schedules/{prisonCode}/activities-by-date-range")
          .queryParam("fromDate", dateRange.start)
          .queryParam("toDate", dateRange.endInclusive)
          .build(prisonCode)
      }
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerSchedule>>())
  }
   */

  fun getLocationsForType(agencyId: String, locationType: String): Mono<List<Location>> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/agencies/{agencyId}/locations/type/{type}")
          .build(agencyId, locationType)
      }
      .retrieve()
      .bodyToMono(typeReference<List<Location>>())
  }

  // Does not check that the invoker has the selected agency in their caseload.
  fun getLocationsForTypeUnrestricted(agencyId: String, locationType: String): Mono<List<Location>> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/agencies/{agencyId}/locations")
          .queryParam("eventType", locationType)
          .build(agencyId)
      }
      .retrieve()
      .bodyToMono(typeReference<List<Location>>())
  }

  fun getLocationGroups(agencyId: String): Mono<List<LocationGroup>> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/agencies/{agencyId}/locations/groups")
          .build(agencyId)
      }
      .retrieve()
      .bodyToMono(typeReference<List<LocationGroup>>())
  }

  fun getLocation(locationId: Long): Mono<Location> =
    prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/locations/{locationId}")
          .build(locationId)
      }
      .retrieve()
      .bodyToMono(typeReference<Location>())

  internal fun <T> UriBuilder.maybeQueryParam(name: String, type: T?) =
    this.queryParamIfPresent(name, Optional.ofNullable(type))

  fun getEducationLevel(domain: String, educationLevelCode: String): Mono<ReferenceCode> =
    prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/reference-domains/domains/{domain}/codes/{educationLevelCode}")
          .build(domain, educationLevelCode)
      }
      .retrieve()
      .bodyToMono(typeReference<ReferenceCode>())
}
