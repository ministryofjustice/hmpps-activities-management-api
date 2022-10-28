package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import java.time.LocalDate

operator fun LocalDate.rangeTo(other: LocalDate) = LocalDateRange(this, other)
