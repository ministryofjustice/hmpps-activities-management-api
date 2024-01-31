package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

fun LocalDateTime.toIsoDateTime(): String = this.format(DateTimeFormatter.ISO_DATE_TIME)

fun LocalDateTime.toMediumFormatStyle(): String = this.format(DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm:ss", Locale.ENGLISH))
