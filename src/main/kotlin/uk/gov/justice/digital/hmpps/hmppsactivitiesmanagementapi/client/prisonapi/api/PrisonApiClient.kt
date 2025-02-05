package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.CourtHearings
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Education
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.LocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import java.time.Duration
import java.time.LocalDate
import java.util.*
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

typealias PrisonLocations = Map<Long, Location>

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

/**
 * Provides a client to the prison API
 *
 * It should be noted where possible we should be using the prisoner search API in favour of prison API.
 */
@Service
class PrisonApiClient(
  private val prisonApiWebClient: WebClient,
  @Value("\${prison.api.retry.max-attempts:2}") private val retryMaxAttempts: Long = 2,
  @Value("\${prison.api.retry.min-backoff-millis:250}") private val retryMinBackoffMillis: Long = 250,
) {

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

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
      .bodyToMono(InmateDetail::class.java)
      .withRetryPolicy()
      .awaitSingle()
  }

  suspend fun getScheduledActivitiesAsync(
    bookingId: Long,
    dateRange: LocalDateRange,
  ): List<PrisonApiScheduledEvent> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/bookings/{bookingId}/activities")
        .queryParam("fromDate", dateRange.start)
        .queryParam("toDate", dateRange.endInclusive)
        .build(bookingId)
    }
    .retrieve()
    .bodyToMono(typeReference<List<PrisonApiScheduledEvent>>())
    .withRetryPolicy()
    .awaitSingle()

  suspend fun getScheduledAppointmentsAsync(
    bookingId: Long,
    dateRange: LocalDateRange,
  ): List<PrisonApiScheduledEvent> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/bookings/{bookingId}/appointments")
        .queryParam("fromDate", dateRange.start)
        .queryParam("toDate", dateRange.endInclusive)
        .build(bookingId)
    }
    .retrieve()
    .bodyToMono(typeReference<List<PrisonApiScheduledEvent>>())
    .withRetryPolicy()
    .awaitSingle()

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
      .bodyToMono(typeReference<List<PrisonerSchedule>>())
      .withRetryPolicy()
      .awaitSingle()
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
      .bodyToMono(CourtHearings::class.java)
      .withRetryPolicy()
      .awaitSingleOrNull()
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
      .bodyToMono(typeReference<List<PrisonerSchedule>>())
      .withRetryPolicy()
      .awaitSingle()
  }

  suspend fun getScheduledVisitsAsync(
    bookingId: Long,
    dateRange: LocalDateRange,
  ): List<PrisonApiScheduledEvent> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/bookings/{bookingId}/visits")
        .queryParam("fromDate", dateRange.start)
        .queryParam("toDate", dateRange.endInclusive)
        .build(bookingId)
    }
    .retrieve()
    .bodyToMono(typeReference<List<PrisonApiScheduledEvent>>())
    .withRetryPolicy()
    .awaitSingle()

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
      .bodyToMono(typeReference<List<PrisonerSchedule>>())
      .withRetryPolicy()
      .awaitSingle()
  }

  suspend fun getScheduledVisitsForLocationAsync(
    prisonCode: String,
    locationId: Long,
    date: LocalDate,
    timeSlot: TimeSlot?,
  ): List<PrisonerSchedule> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/schedules/{prisonCode}/locations/{locationId}/usage/VISIT")
        .queryParam("date", date)
        .maybeQueryParam("timeSlot", timeSlot)
        .build(prisonCode, locationId)
    }
    .retrieve()
    .bodyToMono(typeReference<List<PrisonerSchedule>>())
    .withRetryPolicy()
    .awaitSingle()

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
      .bodyToMono(typeReference<List<PrisonerSchedule>>())
      .withRetryPolicy()
      .awaitSingle()
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
      .bodyToMono(typeReference<List<PrisonerSchedule>>())
      .withRetryPolicy()
      .awaitSingle()
  }

  fun getLocationsForType(agencyId: String, locationType: String): Mono<List<Location>> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/agencies/{agencyId}/locations/type/{type}")
        .build(agencyId, locationType)
    }
    .retrieve()
    .bodyToMono(typeReference<List<Location>>())
    .withRetryPolicy()

  // Does not check that the invoker has the selected agency in their caseload.
  fun getLocationsForTypeUnrestricted(
    agencyId: String,
    locationType: String,
  ): Mono<List<Location>> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/agencies/{agencyId}/locations")
        .queryParam("eventType", locationType)
        .build(agencyId)
    }
    .retrieve()
    .bodyToMono(typeReference<List<Location>>())
    .withRetryPolicy()

  fun getLocationGroups(agencyId: String): Mono<List<LocationGroup>> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/agencies/{agencyId}/locations/groups")
        .build(agencyId)
    }
    .retrieve()
    .bodyToMono(typeReference<List<LocationGroup>>())
    .withRetryPolicy()

  fun getLocation(locationId: Long): Mono<Location> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/locations/{locationId}")
        .build(locationId)
    }
    .retrieve()
    .bodyToMono(typeReference<Location>())
    .withRetryPolicy()

  suspend fun getEventLocationsForPrison(prisonCode: String): PrisonLocations = getEventLocationsAsync(prisonCode).associateBy(Location::locationId)

  suspend fun getLocationAsync(locationId: Long, includeInactive: Boolean = false): Location = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/locations/{locationId}")
        .queryParam("includeInactive", includeInactive)
        .build(locationId)
    }
    .retrieve()
    .bodyToMono(Location::class.java)
    .withRetryPolicy()
    .awaitSingle()

  internal fun <T> UriBuilder.maybeQueryParam(name: String, type: T?) = this.queryParamIfPresent(name, Optional.ofNullable(type))

  fun getReferenceCode(domain: String, code: String): Mono<ReferenceCode> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/reference-domains/domains/{domain}/codes/{code}")
        .build(domain, code)
    }
    .retrieve()
    .bodyToMono(typeReference<ReferenceCode>())
    .withRetryPolicy()

  fun getReferenceCodes(domain: String): List<ReferenceCode> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/reference-domains/domains/{domain}/codes")
        .build(domain)
    }
    .retrieve()
    .bodyToMono(typeReference<List<ReferenceCode>>())
    .withRetryPolicy()
    .block() ?: emptyList()

  fun getEducationLevel(educationLevelCode: String): Mono<ReferenceCode> = getReferenceCode("EDU_LEVEL", educationLevelCode)

  fun getStudyArea(studyAreaCode: String): Mono<ReferenceCode> = getReferenceCode("STUDY_AREA", studyAreaCode)

  fun getScheduleReasons(eventType: String): List<ReferenceCode> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/reference-domains/scheduleReasons")
        .queryParam("eventType", eventType)
        .build()
    }
    .retrieve()
    .bodyToMono(typeReference<List<ReferenceCode>>())
    .withRetryPolicy()
    .block() ?: emptyList()

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
      .withRetryPolicy()
      .block() ?: emptyList()

    return educations
      .filter {
        it.studyArea != null &&
          it.educationLevel != null &&
          (!excludeInFlightCertifications || it.endDate?.isBefore(LocalDate.now()) == true)
      }
      .distinctBy { it.educationLevel + it.studyArea + it.bookingId }
  }

  suspend fun getEventLocationsAsync(prisonCode: String): List<Location> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/agencies/{prisonCode}/eventLocations")
        .build(prisonCode)
    }
    .retrieve()
    .bodyToMono(typeReference<List<Location>>())
    .withRetryPolicy()
    .awaitSingle()

  suspend fun getEventLocationsBookedAsync(prisonCode: String, date: LocalDate, timeSlot: TimeSlot?): List<LocationSummary> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/agencies/{prisonCode}/eventLocationsBooked")
        .queryParam("bookedOnDay", date)
        .maybeQueryParam("timeSlot", timeSlot)
        .build(prisonCode)
    }
    .retrieve()
    .bodyToMono(typeReference<List<LocationSummary>>())
    .withRetryPolicy()
    .awaitSingle()

  fun getMovementsForPrisonersFromPrison(prisonCode: String, prisonerNumbers: Set<String>) = prisonerNumbers.ifNotEmpty {
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
      .withRetryPolicy()
      .block()
  }?.filter { it.fromAgency == prisonCode } ?: emptyList()

  private fun <T> Mono<T>.withRetryPolicy(): Mono<T> = this
    .retryWhen(
      Retry.backoff(retryMaxAttempts, Duration.ofMillis(retryMinBackoffMillis))
        .filter { isRetryable(it) }
        .doBeforeRetry { logRetrySignal(it) }
        .onRetryExhaustedThrow { _, signal ->
          signal.failure()
        },
    )

  private fun isRetryable(it: Throwable): Boolean = it is WebClientRequestException || it.cause is WebClientRequestException

  private fun logRetrySignal(retrySignal: Retry.RetrySignal) {
    val exception = retrySignal.failure()?.cause ?: retrySignal.failure()
    val message = exception.message ?: exception.javaClass.canonicalName
    log.debug("Retrying due to {}, totalRetries: {}", message, retrySignal.totalRetries())
  }
}
