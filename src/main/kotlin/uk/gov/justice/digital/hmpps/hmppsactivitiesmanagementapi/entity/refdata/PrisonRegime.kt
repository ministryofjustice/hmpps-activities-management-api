package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "prison_regime")
data class PrisonRegime(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val prisonRegimeId: Long = 0,

  val prisonCode: String,

  val amStart: LocalTime,

  val amFinish: LocalTime,

  val pmStart: LocalTime,

  val pmFinish: LocalTime,

  val edStart: LocalTime,

  val edFinish: LocalTime,

  val maxDaysToExpiry: Int,
) {
  fun hasExpired(allocation: Allocation) =
    allocation.status(PrisonerStatus.AUTO_SUSPENDED) && hasExpired { allocation.suspendedTime?.toLocalDate() }

  fun hasExpired(predicate: () -> LocalDate?) =
    predicate()?.onOrBefore(LocalDate.now().minusDays(maxDaysToExpiry.toLong())) == true
}
