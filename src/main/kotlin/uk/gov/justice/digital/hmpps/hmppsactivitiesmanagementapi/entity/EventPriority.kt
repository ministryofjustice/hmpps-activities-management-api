package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

// TODO this will need to contain more information e.g. subtype/subcategories.
@Entity
@Table(name = "event_priority")
data class EventPriority(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val eventPriorityId: Long? = null,

  val prisonCode: String,

  @Enumerated(EnumType.STRING)
  val eventType: EventType,

  val priority: Int
)
