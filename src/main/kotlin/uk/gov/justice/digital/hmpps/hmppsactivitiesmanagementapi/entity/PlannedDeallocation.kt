package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "planned_deallocation")
data class PlannedDeallocation(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val plannedDeallocationId: Long = 0,

  @OneToOne
  @JoinColumn(name = "allocation_id", nullable = false)
  var allocation: Allocation,

  var plannedDate: LocalDate,

  var plannedBy: String,

  @Enumerated(EnumType.STRING)
  var plannedReason: DeallocationReason,

  var plannedAt: LocalDateTime = LocalDateTime.now(),
)
