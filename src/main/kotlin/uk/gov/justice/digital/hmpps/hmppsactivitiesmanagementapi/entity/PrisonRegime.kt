package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.onOrBefore
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

  fun timeSlots() = mapOf(
    TimeSlot.AM to Pair(amStart, amFinish),
    TimeSlot.PM to Pair(pmStart, pmFinish),
    TimeSlot.ED to Pair(edStart, edFinish),
  )
}
