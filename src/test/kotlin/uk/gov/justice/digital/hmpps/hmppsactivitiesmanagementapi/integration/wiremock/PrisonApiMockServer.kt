package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.LocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonerTransfer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.read
import java.time.LocalDate

class PrisonApiMockServer : MockServer(8999) {

  fun stubGetScheduledAppointments(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/appointments?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/bookings/appointments-1.json")
            .withStatus(200),
        ),
    )
  }

  fun stubGetScheduledAppointmentsNotFound(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/appointments?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/scheduled-event-404.json")
            .withStatus(404),
        ),
    )
  }

  fun stubGetScheduledAppointmentsForPrisonerNumbers(prisonCode: String, date: LocalDate) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/api/schedules/$prisonCode/appointments?date=$date"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/schedules/appointments-1.json")
            .withStatus(200),
        ),
    )
  }

  fun stubGetScheduledActivities(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/activities?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/scheduled-event-activity-1.json")
            .withStatus(200),
        ),
    )
  }

  fun stubGetScheduledActivitiesWithConnectionReset(bookingId: Long, startDate: LocalDate, endDate: LocalDate, numFails: Int = 1) {
    for (i in 1..numFails) {
      stubFor(
        WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/activities?fromDate=$startDate&toDate=$endDate"))
          .inScenario("Network fail")
          .whenScenarioStateIs(if (i == 1) STARTED else "Fail ${i - 1}")
          .willReturn(
            WireMock.aResponse()
              .withFault(Fault.CONNECTION_RESET_BY_PEER),
          )
          .willSetStateTo("Fail $i"),
      )
    }

    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/activities?fromDate=$startDate&toDate=$endDate"))
        .inScenario("Network fail")
        .whenScenarioStateIs("Fail $numFails")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/scheduled-event-activity-1.json")
            .withStatus(200),
        ),
    )
  }

  fun stubGetScheduledActivitiesNotFound(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/activities?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/scheduled-event-404.json")
            .withStatus(404),
        ),
    )
  }

  fun stubGetScheduledActivitiesForPrisonerNumbers(prisonCode: String, date: LocalDate) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/api/schedules/$prisonCode/activities?date=$date"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/schedules/activities-1.json")
            .withStatus(200),
        ),
    )
  }

  fun stubGetCourtHearings(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/court-hearings?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("prisonapi/bookings/court-hearings-1.json")
            .withStatus(200),
        ),
    )
  }

  fun stubGetCourtHearingsNotFound(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/court-hearings?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("prisonapi/court-hearings-404.json")
            .withStatus(404),
        ),
    )
  }

  fun stubGetCourtEventsForPrisonerNumbers(prisonCode: String, date: LocalDate) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/api/schedules/$prisonCode/courtEvents?date=$date"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/schedules/court-events-1.json")
            .withStatus(200),
        ),
    )
  }

  fun stubGetScheduledVisits(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/visits?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/bookings/visits-1.json")
            .withStatus(200),
        ),
    )
  }

  fun stubGetScheduledVisitsNotFound(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/visits?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/scheduled-event-404.json")
            .withStatus(404),
        ),
    )
  }

  fun stubGetScheduledVisitsForPrisonerNumbers(prisonCode: String, date: LocalDate) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/api/schedules/$prisonCode/visits?date=$date"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/schedules/visits-1.json")
            .withStatus(200),
        ),
    )
  }

  fun stubScheduledVisitsForLocation(prisonCode: String, locationId: Long, date: LocalDate, timeSlot: TimeSlot?, visits: List<PrisonerSchedule>) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/schedules/$prisonCode/locations/$locationId/usage/VISIT?date=$date${timeSlot?.let { "&timeSlot=$it" } ?: ""}"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(visits))
            .withStatus(200),
        ),
    )
  }

  fun stubScheduledVisitsForLocationNotFound(prisonCode: String, locationId: Long, date: LocalDate, timeSlot: TimeSlot?) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/schedules/$prisonCode/locations/$locationId/usage/VISIT?date=$date${timeSlot?.let { "&timeSlot=$it" } ?: ""}"))
        .willReturn(
          WireMock.aResponse()
            .withStatus(404),
        ),
    )
  }

  fun stubGetPrisonerDetails(prisonerNumber: String, jsonFileSuffix: String = "") {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/offenderNo/$prisonerNumber"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/inmate-details-$prisonerNumber$jsonFileSuffix.json")
            .withStatus(200),
        ),
    )
  }

  fun stubGetPrisonerDetails(prisoner: InmateDetail) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/offenderNo/${prisoner.offenderNo}"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(prisoner))
            .withStatus(200),
        ),
    )
  }

  fun stubGetPrisonerDetailsNotFound(prisonerNumber: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/offenderNo/$prisonerNumber?fullInfo=true"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/inmate-details-404.json")
            .withStatus(404),
        ),
    )
  }

  fun stubGetLocationsForType(agencyId: String, locationType: String, jsonResponseFile: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations/type/$locationType"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile(jsonResponseFile)
            .withStatus(200),
        ),
    )
  }

  fun stubGetLocationsForTypeNotFound(agencyId: String, locationType: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations/type/$locationType"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/location-404.json")
            .withStatus(404),
        ),
    )
  }

  fun stubGetLocationsForTypeServerError(agencyId: String, locationType: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations/type/$locationType"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("error-500.json")
            .withStatus(500),
        ),
    )
  }

  fun stubGetLocationsForTypeUnrestricted(agencyId: String, locationType: String, jsonResponseFile: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations?eventType=$locationType"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile(jsonResponseFile)
            .withStatus(200),
        ),
    )
  }

  fun stubGetLocationGroups(agencyId: String, jsonResponseFile: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations/groups"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile(jsonResponseFile)
            .withStatus(200),
        ),
    )
  }

  fun stubGetLocationGroupsNotFound(agencyId: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations/groups"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/location-group-404.json")
            .withStatus(404),
        ),
    )
  }

  fun stubGetLocation(locationId: Long, jsonResponseFile: String, includeInactive: Boolean? = null) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/locations/$locationId" + (includeInactive?.let { "?includeInactive=$includeInactive" } ?: "")))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile(jsonResponseFile)
            .withStatus(200),
        ),
    )
  }

  fun stubGetReferenceCode(domain: String, referenceCode: String, jsonResponseFile: String): ReferenceCode {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/reference-domains/domains/$domain/codes/$referenceCode"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile(jsonResponseFile)
            .withStatus(200),
        ),
    )

    return mapper.read(jsonResponseFile)
  }

  @Deprecated("SAA-2421: In future on DPS Location and not internal location will be used")
  fun stubGetLocationsForAppointments(prisonCode: String, locationId: Long) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$prisonCode/locations?eventType=APP"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(listOf(appointmentLocation(locationId, prisonCode))))
            .withStatus(200),
        ),
    )
  }

  fun stubGetExternalTransfersOnDate(prisonCode: String, prisonerNumbers: Set<String>, date: LocalDate, includeTimes: Boolean = true) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/api/schedules/$prisonCode/externalTransfers?date=$date"))
        .withRequestBody(equalToJson(mapper.writeValueAsString(prisonerNumbers)))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(prisonerNumbers.map { prisonerTransfer(offenderNo = it, date = (if (includeTimes) date else null)) }))
            .withStatus(200),
        ),
    )
  }

  fun stubGetEducationLevels() {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/api/education/prisoners"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody("[]")
            .withStatus(200),
        ),
    )
  }

  fun stubGetEventLocations(prisonCode: String, locations: List<Location>) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$prisonCode/eventLocations"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(locations))
            .withStatus(200),
        ),
    )
  }

  fun stubGetEventLocationsBooked(prisonCode: String, date: LocalDate, timeSlot: TimeSlot?, locations: List<LocationSummary>) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$prisonCode/eventLocationsBooked?bookedOnDay=$date${timeSlot?.let { "&timeSlot=$it" } ?: ""}"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(locations))
            .withStatus(200),
        ),
    )
  }

  fun stubPrisonerMovements(prisonerNumbers: List<String>, movements: List<Movement>) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/api/movements/offenders?latestOnly=false"))
        .withRequestBody(equalToJson(mapper.writeValueAsString(prisonerNumbers)))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(movements))
            .withStatus(200),
        ),
    )
  }
}
