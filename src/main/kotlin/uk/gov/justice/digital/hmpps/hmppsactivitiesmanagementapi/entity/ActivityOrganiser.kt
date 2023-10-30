package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "activity_organiser")
data class ActivityOrganiser(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityOrganiserId: Long = 0,

  @Column(nullable = false)
  val code: String,

  @Column(nullable = false)
  val description: String,
)
