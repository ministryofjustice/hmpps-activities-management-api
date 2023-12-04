package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

fun <T, R> Collection<T>.ifNotEmpty(block: (Collection<T>) -> R): R? =
  this.takeIf { it.isNotEmpty() }?.let { block(this) }

fun <T> Collection<T>.containsAny(otherCollection: Collection<T>): Boolean {
  return this.intersect(otherCollection.toSet()).isNotEmpty()
}

fun <T> Set<T>.intersectIfNotNull(otherSet: Set<T>?): Set<T> {
  return if (otherSet != null) this.intersect(otherSet.toSet()) else this
}
