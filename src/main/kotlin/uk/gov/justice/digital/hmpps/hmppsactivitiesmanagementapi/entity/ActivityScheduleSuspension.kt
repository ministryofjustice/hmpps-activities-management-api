package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import java.time.LocalDate
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table

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
