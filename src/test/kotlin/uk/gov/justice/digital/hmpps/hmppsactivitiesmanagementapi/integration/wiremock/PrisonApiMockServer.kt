package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.adjudicationHearing
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonerTransfer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userCaseLoads
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import wiremock.com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate

class PrisonApiMockServer : WireMockServer(8999) {

  private val mapper = ObjectMapper()

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

  fun stubGetPrisonerDetails(prisonerNumber: String, fullInfo: Boolean = true) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/offenderNo/$prisonerNumber?fullInfo=$fullInfo"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/inmate-details-1.json")
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

  fun stubGetLocationsForTypeUnrestrictedNotFound(agencyId: String, locationType: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/agencies/$agencyId/locations?eventType=$locationType"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile("prisonapi/location-404.json")
            .withStatus(404),
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

  fun stubGetLocation(locationId: Long, jsonResponseFile: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/locations/$locationId"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile(jsonResponseFile)
            .withStatus(200),
        ),
    )
  }

  fun stubGetEducationLevel(domain: String, educationLevelCode: String, jsonResponseFile: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/reference-domains/domains/$domain/codes/$educationLevelCode"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBodyFile(jsonResponseFile)
            .withStatus(200),
        ),
    )
  }

  fun stubGetLocation(locationId: Long, location: Location) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/locations/$locationId"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(location))
            .withStatus(200),
        ),
    )
  }

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

  fun stubGetUserCaseLoads(prisonCode: String) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/users/me/caseLoads?allCaseloads=false"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(userCaseLoads(prisonCode)))
            .withStatus(200),
        ),
    )
  }

  fun stubGetUserDetailsList(usernames: List<String>) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/api/users/list"))
        .withRequestBody(equalToJson(mapper.writeValueAsString(usernames)))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                (usernames.indices).map { userDetail(it + 1L, usernames[it], "TEST${it + 1}", "USER${it + 1}") },
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubGetExternalTransfersOnDate(prisonCode: String, prisonerNumbers: Set<String>, date: LocalDate) {
    stubFor(
      WireMock.post(WireMock.urlEqualTo("/api/schedules/$prisonCode/externalTransfers?date=$date"))
        .withRequestBody(equalToJson(mapper.writeValueAsString(prisonerNumbers)))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(prisonerNumbers.map { prisonerTransfer(offenderNo = it, date = date) }))
            .withStatus(200),
        ),
    )
  }

  fun stubGetAppointmentCategoryReferenceCodes() {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/reference-domains/domains/INT_SCH_RSN/codes"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                listOf(
                  appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
                  appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
                  appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubGetAppointmentScheduleReasons() {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/reference-domains/scheduleReasons?eventType=APP"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                listOf(
                  appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
                  appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
                  appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubAdjudicationHearing(
    prisonCode: String,
    dateRange: LocalDateRange,
    prisonerNumbers: List<String>,
    timeSlot: TimeSlot? = null,
  ) {
    stubFor(
      WireMock.post(
        WireMock.urlEqualTo(
          "/api/offenders/adjudication-hearings?agencyId=$prisonCode&fromDate=${dateRange.start}&toDate=${dateRange.endInclusive}${timeSlot?.let { "&timeSlot=$it" } ?: ""}",
        ),
      )
        .withRequestBody(equalToJson(mapper.writeValueAsString(prisonerNumbers)))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                prisonerNumbers.mapIndexed { hearingId, offenderNo ->
                  adjudicationHearing(
                    prisonCode = prisonCode,
                    offenderNo = offenderNo,
                    hearingId = hearingId.plus(1).toLong(),
                    hearingType = "Governors Hearing Adult",
                    startTime = dateRange.start.atTime(10, 30, 0),
                    eventStatus = "SCH",
                    internalLocationId = 1L,
                    internalLocationDescription = "Governors office",
                  )
                },
              ),
            )
            .withStatus(200),
        ),
    )
  }
}
