package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "activity_schedule_suspension")
data class ActivityScheduleSuspension(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityScheduleSuspensionId: Long = -1,

  @ManyToOne
  @JoinColumn(name = "activity_schedule_id", nullable = false)
  val activitySchedule: ActivitySchedule,

  val suspendedFrom: LocalDate,

  val suspendedUntil: LocalDate? = null,
)
