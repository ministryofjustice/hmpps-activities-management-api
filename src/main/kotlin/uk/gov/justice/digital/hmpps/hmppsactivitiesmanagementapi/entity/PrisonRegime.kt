package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
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

)
