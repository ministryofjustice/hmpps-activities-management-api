package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * Simple helper functions for commonly used assertions
 */

internal infix fun Boolean.isBool(value: Boolean) {
  assertThat(this).isEqualTo(value)
}

internal infix fun LocalDateTime?.isCloseTo(dateTime: LocalDateTime) {
  assertThat(this).isCloseTo(dateTime, within(2, ChronoUnit.SECONDS))
}

internal infix fun LocalDateTime?.isWithinAMinuteOf(dateTime: LocalDateTime) {
  assertThat(this).isCloseTo(dateTime, within(1, ChronoUnit.MINUTES))
}

internal infix fun <T> T.isEqualTo(value: T) {
  assertThat(this).isEqualTo(value)
}

internal infix fun <T> T.isNotEqualTo(value: T) {
  assertThat(this).isNotEqualTo(value)
}

internal infix fun <T> Collection<T>.hasSize(size: Int) {
  assertThat(this).hasSize(size)
}

internal inline infix fun <reified T> Collection<T>.containsExactly(value: Collection<T>) {
  assertThat(this).containsExactly(*value.toTypedArray())
}

internal inline infix fun <reified T> Collection<T>.containsExactlyInAnyOrder(value: Collection<T>) {
  assertThat(this).containsExactlyInAnyOrder(*value.toTypedArray())
}

internal infix fun String.startsWith(prefix: String) {
  assertThat(this).startsWith(prefix)
}
