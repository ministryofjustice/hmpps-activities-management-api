package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter

operator fun LocalDate.rangeTo(other: LocalDate) = LocalDateRange(this, other)

fun LocalDate.between(from: LocalDate, to: LocalDate?) = this >= from && (to == null || this <= to)

fun LocalDate?.onOrBefore(date: LocalDate) = this != null && this <= date
fun LocalDate?.onOrAfter(date: LocalDate) = this != null && this >= date

fun LocalDate.toIsoDate(): String = this.format(DateTimeFormatter.ISO_DATE)

fun Int.daysAgo(): LocalDate = require(this > 0) { "Days ago must be positive" }.let { LocalDate.now().minusDays(this.toLong()) }
fun Int.weeksAgo(): LocalDate = require(this > 0) { "Weeks ago must be positive" }.let { LocalDate.now().minusWeeks(this.toLong()) }
fun Int.daysFromNow(): LocalDate = require(this > 0) { "Days from now must be positive" }.let { LocalDate.now().plusDays(this.toLong()) }
