package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentsDataSource
import java.time.LocalDate

@Entity
@Table(name = "rollout_prison")
data class RolloutPrison(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val rolloutPrisonId: Long = -1,

  val code: String,

  val description: String,

  var active: Boolean = false,

  val rolloutDate: LocalDate?,

  @Enumerated(STRING)
  val appointmentsDataSource: AppointmentsDataSource,
)
