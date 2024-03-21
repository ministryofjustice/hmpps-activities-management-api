package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders

abstract class MockServer(port: Int) : WireMockServer(port) {

  val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

  fun stubHealthPing(status: Int) {
    val stat = if (status == 200) "UP" else "DOWN"
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
          .withBody("""{"status": "$stat"}""")
          .withStatus(status),
      ),
    )
  }
}
