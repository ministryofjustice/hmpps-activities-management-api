package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import reactor.core.publisher.Mono
import reactor.util.context.Context
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.RetryApiService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.LocationGroup
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Education
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.LocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.ifNotEmpty
import java.time.LocalDate
import java.util.*

typealias PrisonLocations = Map<Long, Location>

inline fun <reified T : Any> typeReference() = object : ParameterizedTypeReference<T>() {}

/**
 * Provides a client to the prison API
 *
 * It should be noted where possible we should be using the prisoner search API in favour of prison API.
 */
@Service
class PrisonApiClient(
  private val prisonApiWebClient: WebClient,
  retryApiService: RetryApiService,
  @Value("\${prison.api.retry.max-retries:2}") private val maxRetryAttempts: Long = 2,
  @Value("\${prison.api.retry.backoff-millis:250}") private val backoffMillis: Long = 250,
) {

  private companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  private val backoffSpec = retryApiService.getBackoffSpec(maxRetryAttempts, backoffMillis)

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
      .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/bookings/offenderNo/{prisonerNumber}")))
      .awaitSingle()
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
      .bodyToMono(typeReference<List<PrisonerSchedule>>())
      .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/schedules/{prisonCode}/appointments")))
      .awaitSingle()
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
      .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/schedules/{prisonCode}/courtEvents")))
      .awaitSingle()
  }

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
      .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/schedules/{prisonCode}/visits")))
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
    .doOnError { error -> log.info("Error looking up visits for location for $prisonCode $locationId", error) }
    .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.just(emptyList()) }
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/schedules/{prisonCode}/locations/{locationId}/usage/VISIT")))
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
      .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/schedules/{prisonCode}/activities")))
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
      .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/schedules/{agencyId}/externalTransfers")))
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
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/agencies/{agencyId}/locations/type/{type}")))

  fun getLocationGroups(agencyId: String): Mono<List<LocationGroup>> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/agencies/{agencyId}/locations/groups")
        .build(agencyId)
    }
    .retrieve()
    .bodyToMono(typeReference<List<LocationGroup>>())
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/agencies/{agencyId}/locations/groups")))

  suspend fun getEventLocationsForPrison(prisonCode: String): PrisonLocations = getEventLocationsAsync(prisonCode).associateBy(Location::locationId)

  internal fun <T> UriBuilder.maybeQueryParam(name: String, type: T?) = this.queryParamIfPresent(name, Optional.ofNullable(type))

  fun getReferenceCode(domain: String, code: String): Mono<ReferenceCode> = prisonApiWebClient.get()
    .uri { uriBuilder: UriBuilder ->
      uriBuilder
        .path("/api/reference-domains/domains/{domain}/codes/{code}")
        .build(domain, code)
    }
    .retrieve()
    .bodyToMono(typeReference<ReferenceCode>())
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/reference-domains/domains/{domain}/codes/{code}")))

  fun getEducationLevel(educationLevelCode: String): Mono<ReferenceCode> = getReferenceCode("EDU_LEVEL", educationLevelCode)

  fun getStudyArea(studyAreaCode: String): Mono<ReferenceCode> = getReferenceCode("STUDY_AREA", studyAreaCode)

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
      .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/education/prisoners")))
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
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/agencies/{prisonCode}/eventLocations")))
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
    .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/agencies/{prisonCode}/eventLocationsBooked")))
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
      .retryWhen(backoffSpec.withRetryContext(Context.of("api", "prison-api", "path", "/api/movements/offenders")))
      .block()
  }?.filter { it.fromAgency == prisonCode } ?: emptyList()
}
