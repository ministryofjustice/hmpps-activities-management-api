package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api

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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.OffenderNonAssociationDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.OffenderNonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ScheduledEvent as PrisonApiScheduledEvent

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

@Service
class PrisonApiClient(private val prisonApiWebClient: WebClient) {

  fun getPrisonerDetails(prisonerNumber: String, fullInfo: Boolean = true, extraInfo: Boolean? = null): Mono<InmateDetail> {
    return prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/bookings/offenderNo/{prisonerNumber}")
          .queryParam("fullInfo", fullInfo)
          .maybeQueryParam("extraInfo", extraInfo)
          .build(prisonerNumber)
      }
      .retrieve()
      .bodyToMono(typeReference<InmateDetail>())
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
  ): CourtHearings =
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

  suspend fun getOffenderAdjudications(
    agencyId: String,
    dateRange: LocalDateRange,
    prisonerNumbers: Set<String>,
    timeSlot: TimeSlot? = null,
  ): List<OffenderAdjudicationHearing> =
    prisonApiWebClient.post()
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

  fun getUserDetailsList(usernames: List<String>): List<UserDetail> {
    return prisonApiWebClient.post()
      .uri("/api/users/list")
      .bodyValue(usernames)
      .retrieve()
      .bodyToMono(typeReference<List<UserDetail>>())
      .block() ?: emptyList()
  }

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

  fun getOffenderNonAssociations(
    prisonerNumber: String,
    excludeExpired: Boolean = true,
  ): List<OffenderNonAssociationDetail>? {
    val nonAssociationDetails = prisonApiWebClient.get()
      .uri { uriBuilder: UriBuilder ->
        uriBuilder
          .path("/api/offenders/{offenderNo}/non-association-details")
          .build(prisonerNumber)
      }
      .retrieve()
      .bodyToMono(typeReference<OffenderNonAssociationDetails>())
      .block()

    return nonAssociationDetails?.nonAssociations?.filter {
      !excludeExpired || it.expiryDate == null || LocalDateTime.parse(it.expiryDate).isAfter(LocalDateTime.now())
    }
  }
}
