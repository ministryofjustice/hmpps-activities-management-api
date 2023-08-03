package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal infix fun LocalDateTime.isCloseTo(dateTime: LocalDateTime) {
  assertThat(this).isCloseTo(dateTime, within(2, ChronoUnit.SECONDS))
}

internal infix fun <T> T.isEqualTo(value: T) {
  assertThat(this).isEqualTo(value)
}

internal infix fun <T> Collection<T>.hasSize(size: Int) {
  assertThat(this).hasSize(size)
}
