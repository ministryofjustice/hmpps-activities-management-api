package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

fun LocalDateTime.toIsoDateTime(): String = this.format(DateTimeFormatter.ISO_DATE_TIME)

fun LocalDateTime.toMediumFormatStyle(): String = this.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
