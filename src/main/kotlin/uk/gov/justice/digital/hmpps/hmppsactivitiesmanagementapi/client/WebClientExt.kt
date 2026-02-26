package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client

import org.springframework.web.util.UriBuilder

fun UriBuilder.maybeQueryParam(name: String, value: Any?): UriBuilder = apply { if (value != null) queryParam(name, value) }
