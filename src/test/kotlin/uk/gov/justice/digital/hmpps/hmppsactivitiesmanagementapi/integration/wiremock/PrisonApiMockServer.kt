package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import java.time.LocalDate

class PrisonApiMockServer : WireMockServer(8999) {

  fun stubGetScheduledAppointments(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/appointments?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/bookings/appointments-1.json")
            .withStatus(200)
        )
    )
  }

  fun stubGetScheduledAppointmentsNotFound(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/appointments?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/scheduled-event-404.json")
            .withStatus(404)
        )
    )
  }

  fun stubGetScheduledAppointmentsForPrisonerNumbers(prisonCode: String, date: LocalDate) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/api/schedules/$prisonCode/appointments?date=$date"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/schedules/appointments-1.json")
            .withStatus(200)
        )
    )
  }

  fun stubGetScheduledActivities(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/activities?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/scheduled-event-activity-1.json")
            .withStatus(200)
        )
    )
  }

  fun stubGetScheduledActivitiesNotFound(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/activities?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/scheduled-event-404.json")
            .withStatus(404)
        )
    )
  }

  fun stubGetCourtHearings(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/court-hearings?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("prisonapi/bookings/court-hearings-1.json")
            .withStatus(200)
        )
    )
  }

  fun stubGetCourtHearingsNotFound(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/court-hearings?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .withBodyFile("prisonapi/court-hearings-404.json")
            .withStatus(404)
        )
    )
  }

  fun stubGetCourtEventsForPrisonerNumbers(prisonCode: String, date: LocalDate) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/api/schedules/$prisonCode/courtEvents?date=$date"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/schedules/court-events-1.json")
            .withStatus(200)
        )
    )
  }

  fun stubGetScheduledVisits(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/visits?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/bookings/visits-1.json")
            .withStatus(200)
        )
    )
  }

  fun stubGetScheduledVisitsNotFound(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/visits?fromDate=$startDate&toDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/scheduled-event-404.json")
            .withStatus(404)
        )
    )
  }

  fun stubGetScheduledVisitsForPrisonerNumbers(prisonCode: String, date: LocalDate) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/api/schedules/$prisonCode/visits?date=$date"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/schedules/visits-1.json")
            .withStatus(200)
        )
    )
  }

  fun stubGetPrisonerDetails(prisonerNumber: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/offenderNo/$prisonerNumber?fullInfo=true"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/inmate-details-1.json")
            .withStatus(200)
        )
    )
  }

  fun stubGetPrisonerDetailsNotFound(prisonerNumber: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/offenderNo/$prisonerNumber?fullInfo=true"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/inmate-details-404.json")
            .withStatus(404)
        )
    )
  }

  fun stubGetLocationsForType(agencyId: String, locationType: String, jsonResponseFile: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations/type/$locationType"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile(jsonResponseFile)
            .withStatus(200)
        )
    )
  }

  fun stubGetLocationsForTypeNotFound(agencyId: String, locationType: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations/type/$locationType"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/location-404.json")
            .withStatus(404)
        )
    )
  }

  fun stubGetLocationsForTypeServerError(agencyId: String, locationType: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations/type/$locationType"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("error-500.json")
            .withStatus(500)
        )
    )
  }

  fun stubGetLocationsForTypeUnrestricted(agencyId: String, locationType: String, jsonResponseFile: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations?eventType=$locationType"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile(jsonResponseFile)
            .withStatus(200)
        )
    )
  }

  fun stubGetLocationsForTypeUnrestrictedNotFound(agencyId: String, locationType: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations?eventType=$locationType"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/location-404.json")
            .withStatus(404)
        )
    )
  }

  fun stubGetLocationGroups(agencyId: String, jsonResponseFile: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations/groups"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile(jsonResponseFile)
            .withStatus(200)
        )
    )
  }

  fun stubGetLocationGroupsNotFound(agencyId: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations/groups"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/location-group-404.json")
            .withStatus(404)
        )
    )
  }
}
