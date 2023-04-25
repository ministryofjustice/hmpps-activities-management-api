package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import java.time.LocalDate

operator fun LocalDate.rangeTo(other: LocalDate) = LocalDateRange(this, other)

fun LocalDate.between(from: LocalDate, to: LocalDate?) = this >= from && (to == null || this <= to)

fun LocalDate.onOrBefore(date: LocalDate) = this <= date
