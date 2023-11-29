package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventTier

@Repository
interface EventTierRepository : JpaRepository<EventTier, Long> {
  fun findByCode(code: String): EventTier?
}

fun EventTierRepository.findByCodeOrThrowIllegalArgument(code: String) =
  this.findByCode(code) ?: throw IllegalArgumentException("Event tier \"$code\" not found")
