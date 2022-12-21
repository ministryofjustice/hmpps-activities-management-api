package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import javax.persistence.EntityNotFoundException

inline fun <reified T> JpaRepository<T, Long>.findOrThrowIllegalArgument(id: Long): T =
  this.findById(id).orElseThrow { IllegalArgumentException("${T::class.java.simpleName.spaceOut()} $id not found") }

inline fun <reified T> JpaRepository<T, Long>.findOrThrowNotFound(id: Long): T =
  this.findById(id).orElseThrow { EntityNotFoundException("${T::class.java.simpleName.spaceOut()} $id not found") }

fun String.spaceOut() = "[A-Z]".toRegex().replace(this) { " ${it.value}" }.trim()
