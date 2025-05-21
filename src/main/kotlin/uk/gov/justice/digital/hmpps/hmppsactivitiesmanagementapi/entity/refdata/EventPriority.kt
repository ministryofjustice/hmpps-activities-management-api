package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.envers.Audited

@Entity
@Audited
@Table(name = "event_priority")
data class EventPriority(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val eventPriorityId: Long = 0,

  val prisonCode: String,

  @Enumerated(EnumType.STRING)
  val eventType: EventType,

  @Enumerated(EnumType.STRING)
  val eventCategory: EventCategory? = null,

  val priority: Int,
)
