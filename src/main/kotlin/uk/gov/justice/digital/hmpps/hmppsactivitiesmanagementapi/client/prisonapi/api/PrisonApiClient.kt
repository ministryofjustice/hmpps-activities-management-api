package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api

import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.UserDetail
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

  suspend fun getScheduledActivitiesAsync(bookingId: Long, dateRange: LocalDateRange): List<PrisonApiScheduledEvent> =
    prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/{bookingId}/activities")
          .queryParam("fromDate", dateRange.start)
          .queryParam("toDate", dateRange.endInclusive)
          .build(bookingId)
      }
      .retrieve()
      .awaitBody()

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

  suspend fun getScheduledAppointmentsAsync(bookingId: Long, dateRange: LocalDateRange): List<PrisonApiScheduledEvent> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/{bookingId}/appointments")
          .queryParam("fromDate", dateRange.start)
          .queryParam("toDate", dateRange.endInclusive)
          .build(bookingId)
      }
      .retrieve()
      .awaitBody()
  }

  fun getScheduledAppointmentsForPrisonerNumbers(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?,
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

  suspend fun getScheduledCourtHearingsAsync(bookingId: Long, dateRange: LocalDateRange): CourtHearings =
    prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/{bookingId}/court-hearings")
          .queryParam("fromDate", dateRange.start)
          .queryParam("toDate", dateRange.endInclusive)
          .build(bookingId)
      }
      .retrieve()
      .awaitBody()

  fun getScheduledCourtEventsForPrisonerNumbers(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?,
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

  suspend fun getScheduledCourtEventsForPrisonerNumbersAsync(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?,
  ): List<PrisonerSchedule> =
    prisonApiWebClient.post()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/schedules/{prisonCode}/courtEvents")
          .maybeQueryParam("date", date)
          .maybeQueryParam("timeSlot", timeSlot)
          .build(prisonCode)
      }
      .bodyValue(prisonerNumbers)
      .retrieve()
      .awaitBody()

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

  suspend fun getScheduledVisitsAsync(bookingId: Long, dateRange: LocalDateRange): List<PrisonApiScheduledEvent> =
    prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/{bookingId}/visits")
          .queryParam("fromDate", dateRange.start)
          .queryParam("toDate", dateRange.endInclusive)
          .build(bookingId)
      }
      .retrieve()
      .awaitBody()

  fun getScheduledVisitsForPrisonerNumbers(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?,
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

  suspend fun getScheduledVisitsForPrisonerNumbersAsync(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?,
  ): List<PrisonerSchedule> =
    prisonApiWebClient.post()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/schedules/{prisonCode}/visits")
          .maybeQueryParam("date", date)
          .maybeQueryParam("timeSlot", timeSlot)
          .build(prisonCode)
      }
      .bodyValue(prisonerNumbers)
      .retrieve()
      .awaitBody()

  fun getScheduledActivitiesForPrisonerNumbers(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?,
  ): Mono<List<PrisonerSchedule>> =
    prisonApiWebClient.post()
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

  suspend fun getScheduledActivitiesForPrisonerNumbersAsync(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?,
  ): List<PrisonerSchedule> =
    prisonApiWebClient.post()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/schedules/{prisonCode}/activities")
          .maybeQueryParam("date", date)
          .maybeQueryParam("timeSlot", timeSlot)
          .build(prisonCode)
      }
      .bodyValue(prisonerNumbers)
      .retrieve()
      .awaitBody()

  fun getExternalTransfersOnDate(
    agencyId: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
  ): Mono<List<PrisonerSchedule>> =
    prisonApiWebClient.post()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/schedules/{agencyId}/externalTransfers")
          .queryParam("date", date)
          .build(agencyId)
      }
      .bodyValue(prisonerNumbers)
      .retrieve()
      .bodyToMono(typeReference<List<PrisonerSchedule>>())

  suspend fun getExternalTransfersOnDateAsync(
    agencyId: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
  ): List<PrisonerSchedule> =
    prisonApiWebClient.post()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/schedules/{agencyId}/externalTransfers")
          .queryParam("date", date)
          .build(agencyId)
      }
      .bodyValue(prisonerNumbers)
      .retrieve()
      .awaitBody()

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

  fun getReferenceCode(domain: String, code: String): Mono<ReferenceCode> =
    prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/reference-domains/domains/{domain}/codes/{code}")
          .build(domain, code)
      }
      .retrieve()
      .bodyToMono(typeReference<ReferenceCode>())

  fun getEducationLevel(educationLevelCode: String): Mono<ReferenceCode> =
    getReferenceCode("EDU_LEVEL", educationLevelCode)

  fun getUserDetailsList(usernames: List<String>): List<UserDetail> {
    return prisonApiWebClient.post()
      .uri("/api/users/list")
      .bodyValue(usernames)
      .retrieve()
      .bodyToMono(typeReference<List<UserDetail>>())
      .block() ?: emptyList()
  }
}
