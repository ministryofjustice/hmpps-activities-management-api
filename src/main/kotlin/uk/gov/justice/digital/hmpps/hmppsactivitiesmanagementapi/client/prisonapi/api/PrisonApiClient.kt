package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api

import kotlinx.coroutines.runBlocking
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.OffenderAdjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Education
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.LocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import java.time.LocalDate
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

typealias PrisonLocations = Map<Long, Location>

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

/**
 * Provides a client to the prison API
 *
 * It should be noted where possible we should be using the prisoner search API in favour of prison API.
 */
@Service
class PrisonApiClient(private val prisonApiWebClient: WebClient) {

  /**
   * Returns a minimal view of the prisoner's attributes in the [InmateDetail].
   *
   * The rationale for a minimal view is down to what we actually need in terms of information but also to reduce the
   * load on the prison API. Asking for more information than we actually need puts unnecessary load on the prison API.
   *
   * With the minimal view 'status' isn't populated but 'activeFlag' is. This means you can check if a prisoner is
   * active at a prison but not say if they are IN or OUT of the prison at the time.
   */
  fun getPrisonerDetailsLite(prisonerNumber: String): InmateDetail = runBlocking {
    prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/offenderNo/{prisonerNumber}")
          .build(prisonerNumber)
      }
      .retrieve()
      .awaitBody()
  }

  suspend fun getScheduledActivitiesAsync(
    bookingId: Long,
    dateRange: LocalDateRange,
  ): List<PrisonApiScheduledEvent> =
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

  suspend fun getScheduledAppointmentsAsync(
    bookingId: Long,
    dateRange: LocalDateRange,
  ): List<PrisonApiScheduledEvent> {
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

  suspend fun getScheduledAppointmentsForPrisonerNumbersAsync(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?,
  ): List<PrisonerSchedule> {
    if (prisonerNumbers.isEmpty()) return emptyList()
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
      .awaitBody()
  }

  suspend fun getScheduledCourtHearingsAsync(
    bookingId: Long,
    dateRange: LocalDateRange,
  ): CourtHearings? {
    if (dateRange.isEmpty()) return null
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/{bookingId}/court-hearings")
          .queryParam("fromDate", dateRange.start)
          .queryParam("toDate", dateRange.endInclusive)
          .build(bookingId)
      }
      .retrieve()
      .awaitBody()
  }

  suspend fun getScheduledCourtEventsForPrisonerNumbersAsync(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?,
  ): List<PrisonerSchedule> {
    if (prisonerNumbers.isEmpty()) return emptyList()
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
      .awaitBody()
  }

  suspend fun getScheduledVisitsAsync(
    bookingId: Long,
    dateRange: LocalDateRange,
  ): List<PrisonApiScheduledEvent> =
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

  suspend fun getScheduledVisitsForPrisonerNumbersAsync(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?,
  ): List<PrisonerSchedule> {
    if (prisonerNumbers.isEmpty()) return emptyList()
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
      .awaitBody()
  }

  suspend fun getScheduledVisitsForLocationAsync(
    prisonCode: String,
    locationId: Long,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): List<PrisonerSchedule> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/schedules/{prisonCode}/locations/{locationId}/usage/VISIT")
          .queryParam("date", date)
          .maybeQueryParam("timeSlot", timeSlot)
          .build(prisonCode, locationId)
      }
      .retrieve()
      .awaitBody()
  }

  suspend fun getScheduledActivitiesForPrisonerNumbersAsync(
    prisonCode: String,
    prisonerNumbers: Set<String>,
    date: LocalDate?,
    timeSlot: TimeSlot?,
  ): List<PrisonerSchedule> {
    if (prisonerNumbers.isEmpty()) return emptyList()
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
      .awaitBody()
  }

  suspend fun getExternalTransfersOnDateAsync(
    agencyId: String,
    prisonerNumbers: Set<String>,
    date: LocalDate,
  ): List<PrisonerSchedule> {
    if (prisonerNumbers.isEmpty()) return emptyList()
    return prisonApiWebClient.post()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/schedules/{agencyId}/externalTransfers")
          .queryParam("date", date)
          .build(agencyId)
      }
      .bodyValue(prisonerNumbers)
      .retrieve()
      .awaitBody()
  }

  suspend fun getOffenderAdjudications(
    agencyId: String,
    dateRange: LocalDateRange,
    prisonerNumbers: Set<String>,
    timeSlot: TimeSlot? = null,
  ): List<OffenderAdjudicationHearing> {
    return prisonApiWebClient.post()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/offenders/adjudication-hearings")
          .queryParam("agencyId", agencyId)
          .queryParam("fromDate", dateRange.start)
          .queryParam("toDate", dateRange.endInclusive)
          .maybeQueryParam("timeSlot", timeSlot)
          .build()
      }
      .bodyValue(prisonerNumbers)
      .retrieve()
      .awaitBody()
  }

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
  fun getLocationsForTypeUnrestricted(
    agencyId: String,
    locationType: String,
  ): Mono<List<Location>> {
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

  suspend fun getEventLocationsForPrison(prisonCode: String): PrisonLocations =
    getEventLocationsAsync(prisonCode).associateBy(Location::locationId)

  suspend fun getLocationAsync(locationId: Long, includeInactive: Boolean = false): Location =
    prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/locations/{locationId}")
          .queryParam("includeInactive", includeInactive)
          .build(locationId)
      }
      .retrieve()
      .awaitBody()

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

  fun getReferenceCodes(domain: String): List<ReferenceCode> =
    prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/reference-domains/domains/{domain}/codes")
          .build(domain)
      }
      .retrieve()
      .bodyToMono(typeReference<List<ReferenceCode>>())
      .block() ?: emptyList()

  fun getEducationLevel(educationLevelCode: String): Mono<ReferenceCode> =
    getReferenceCode("EDU_LEVEL", educationLevelCode)

  fun getStudyArea(studyAreaCode: String): Mono<ReferenceCode> =
    getReferenceCode("STUDY_AREA", studyAreaCode)

  fun getScheduleReasons(eventType: String): List<ReferenceCode> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/reference-domains/scheduleReasons")
          .queryParam("eventType", eventType)
          .build()
      }
      .retrieve()
      .bodyToMono(typeReference<List<ReferenceCode>>())
      .block() ?: emptyList()
  }

  fun getEducationLevels(
    prisonerNumbers: List<String>,
    excludeInFlightCertifications: Boolean = true,
  ): List<Education> {
    val educations = prisonApiWebClient
      .post()
      .uri("/api/education/prisoners")
      .bodyValue(prisonerNumbers)
      .retrieve()
      .bodyToMono(typeReference<List<Education>>())
      .block() ?: emptyList()

    return educations
      .filter {
        it.studyArea != null &&
          it.educationLevel != null &&
          (!excludeInFlightCertifications || it.endDate?.isBefore(LocalDate.now()) == true)
      }
      .distinctBy { it.educationLevel + it.studyArea + it.bookingId }
  }

  suspend fun getEventLocationsAsync(prisonCode: String): List<Location> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/agencies/{prisonCode}/eventLocations")
          .build(prisonCode)
      }
      .retrieve()
      .awaitBody()
  }

  suspend fun getEventLocationsBookedAsync(prisonCode: String, date: LocalDate, timeSlot: TimeSlot?): List<LocationSummary> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/agencies/{prisonCode}/eventLocationsBooked")
          .queryParam("bookedOnDay", date)
          .maybeQueryParam("timeSlot", timeSlot)
          .build(prisonCode)
      }
      .retrieve()
      .awaitBody()
  }

  fun getMovementsForPrisonersFromPrison(prisonCode: String, prisonerNumbers: Set<String>) =
    prisonerNumbers.ifNotEmpty {
      prisonApiWebClient
        .post()
        .uri { uriBuilder: UriBuilder ->
          uriBuilder
            .path("/api/movements/offenders")
            .queryParam("latestOnly", false)
            .build()
        }
        .bodyValue(prisonerNumbers)
        .retrieve()
        .bodyToMono(typeReference<List<Movement>>())
        .block()
    }?.filter { it.fromAgency == prisonCode } ?: emptyList()
}
