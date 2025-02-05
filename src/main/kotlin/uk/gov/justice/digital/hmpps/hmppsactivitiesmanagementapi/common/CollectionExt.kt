package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

fun <T, R> Collection<T>.ifNotEmpty(block: (Collection<T>) -> R): R? = this.takeIf { it.isNotEmpty() }?.let { block(this) }

fun <T> Collection<T>.containsAny(otherCollection: Collection<T>): Boolean = this.intersect(otherCollection.toSet()).isNotEmpty()

/**
 * Conversion is not guaranteed, only use when you know (or at least expect) all types to be the same type.
 */
@Suppress("UNCHECKED_CAST")
inline fun <reified T> List<*>.asListOfType() = this as List<T>
