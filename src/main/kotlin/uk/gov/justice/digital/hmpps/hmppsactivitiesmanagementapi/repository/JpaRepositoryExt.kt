package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import javax.persistence.EntityNotFoundException

inline fun <reified T> JpaRepository<T, Long>.findOrThrowIllegalArgument(id: Long): T =
  this.findById(id).orElseThrow { IllegalArgumentException("${T::class.java.simpleName} $id not found") }

inline fun <reified T> JpaRepository<T, Long>.findOrThrowNotFound(id: Long): T =
  this.findById(id).orElseThrow { EntityNotFoundException("${T::class.java.simpleName} $id not found") }
