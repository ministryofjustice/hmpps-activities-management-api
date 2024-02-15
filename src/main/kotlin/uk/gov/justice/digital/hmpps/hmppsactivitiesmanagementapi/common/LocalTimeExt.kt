package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import java.time.LocalTime
import java.time.format.DateTimeFormatter

fun LocalTime.toIsoTime(): String = this.format(DateTimeFormatter.ISO_TIME)

fun LocalTime.onOrAfter(that: LocalTime) = this >= that
