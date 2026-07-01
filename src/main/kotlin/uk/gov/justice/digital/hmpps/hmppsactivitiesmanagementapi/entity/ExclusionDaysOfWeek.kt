package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.envers.AuditTable
import org.hibernate.envers.Audited
import java.time.DayOfWeek

@Entity
@Audited
@AuditTable(value = "exclusion_days_of_week_aud")
@Table(name = "exclusion_days_of_week")
data class ExclusionDaysOfWeek(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long = 0,

  @ManyToOne
  @JoinColumn(name = "exclusion_id", nullable = false)
  val exclusion: Exclusion,

  @Enumerated(EnumType.STRING)
  val dayOfWeek: DayOfWeek,
)
