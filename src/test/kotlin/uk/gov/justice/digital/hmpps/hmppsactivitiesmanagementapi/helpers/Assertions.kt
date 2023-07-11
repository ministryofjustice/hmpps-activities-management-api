package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers

import org.assertj.core.api.Assertions.assertThat

internal infix fun <T> T.isEqualTo(value: T) {
  assertThat(this).isEqualTo(value)
}

internal infix fun <T> Collection<T>.hasSize(size: Int) {
  assertThat(this).hasSize(size)
}
