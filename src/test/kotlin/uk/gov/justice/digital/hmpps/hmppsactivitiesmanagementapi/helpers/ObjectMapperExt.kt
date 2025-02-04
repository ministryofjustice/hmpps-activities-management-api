package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

internal inline fun <reified T> ObjectMapper.read(pathToFile: String): T = this.readValue(this::class.java.getResource("/__files/$pathToFile"), object : TypeReference<T>() {})
