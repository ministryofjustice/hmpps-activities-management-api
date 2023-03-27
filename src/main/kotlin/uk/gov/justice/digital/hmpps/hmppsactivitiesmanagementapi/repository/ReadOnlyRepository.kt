package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.Repository
import java.util.Optional

@NoRepositoryBean
interface ReadOnlyRepository<T, ID> : Repository<T, ID> {
  fun findById(id: ID): Optional<T>
}

inline fun <reified T, ID> ReadOnlyRepository<T, ID>.findOrThrowNotFound(id: ID): T =
  this.findById(id).orElseThrow { EntityNotFoundException("${T::class.java.simpleName.spaceOut()} $id not found") }
