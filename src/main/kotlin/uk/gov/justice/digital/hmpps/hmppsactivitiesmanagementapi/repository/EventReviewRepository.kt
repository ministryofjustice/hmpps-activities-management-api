package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReview

@Repository
interface EventReviewRepository : JpaRepository<EventReview, Long>, JpaSpecificationExecutor<EventReview> {
  fun findByPrisonCodeAndPrisonerNumber(prisonCode: String, prisonerNumber: String): List<EventReview>

  @Query(value = "UPDATE EventReview e SET e.prisonerNumber = :newNumber WHERE e.prisonerNumber = :oldNumber")
  @Modifying
  fun mergeOffender(oldNumber: String, newNumber: String)
}
