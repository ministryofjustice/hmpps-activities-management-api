package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventOrganiser

@Repository
interface EventOrganiserRepository : JpaRepository<EventOrganiser, Long> {
  fun findByCode(code: String): EventOrganiser?
}

fun EventOrganiserRepository.findByCodeOrThrowIllegalArgument(code: String) = this.findByCode(code) ?: throw IllegalArgumentException("Event organiser \"$code\" not found")
