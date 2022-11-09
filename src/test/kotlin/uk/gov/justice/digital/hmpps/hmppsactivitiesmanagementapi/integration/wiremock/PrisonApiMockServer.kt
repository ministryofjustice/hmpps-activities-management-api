package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import java.io.File
import java.nio.file.Files
import java.time.LocalDate
import java.util.stream.Collectors

class PrisonApiMockServer : WireMockServer(8999) {

  private val objectMapper = getObjectMapper()

  fun stubGetScheduledAppointments(bookingId: Long, startDate: LocalDate, endDate: LocalDate) {
    stubFor(
      WireMock.get(WireMock.urlEqualTo("/api/bookings/$bookingId/appointments?startDate=$startDate&endDate=$endDate"))
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              readFile("/fixtures/prisonapi/scheduled-event-1.json")
            )
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
            .withBody(
              readFile("/fixtures/prisonapi/inmate-details-1.json")
            )
            .withStatus(200)
        )
    )
  }

  private fun readFile(uri: String): String? {
    val file = File(this::class.java.getResource(uri)!!.file)
    val lines = Files.lines(file.toPath())
    val data = lines.collect(Collectors.joining("\n"))
    lines.close()
    return data
  }

  private fun getObjectMapper(): ObjectMapper {
    val objectMapper = ObjectMapper()
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    objectMapper.configure(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES, false)
    objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    return objectMapper
  }
}
