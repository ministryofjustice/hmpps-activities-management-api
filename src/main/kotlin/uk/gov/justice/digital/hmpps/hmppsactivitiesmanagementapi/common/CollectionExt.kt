package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

fun <T, R> Collection<T>.ifNotEmpty(block: (Collection<T>) -> R): R? =
  this.takeIf { it.isNotEmpty() }?.let { block(this) }
