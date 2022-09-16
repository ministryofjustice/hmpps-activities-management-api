package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.hibernate.annotations.Fetch
import org.hibernate.annotations.FetchMode
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.OneToMany
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "activity")
data class Activity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityId: Long? = null,

  val prisonCode: String,

  @OneToOne
  @JoinColumn(name = "activity_category_id", nullable = false)
  val activityCategory: ActivityCategory,

  @OneToOne
  @JoinColumn(name = "activity_tier", nullable = false)
  val activityTier: ActivityTier,

  @OneToMany(mappedBy = "activity", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  val eligibilityRules: MutableList<ActivityEligibility> = mutableListOf(),

  @OneToMany(mappedBy = "activity", fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @Fetch(FetchMode.SUBSELECT)
  val sessions: MutableList<ActivitySession> = mutableListOf(),

  @OneToOne(fetch = FetchType.EAGER, cascade = [CascadeType.ALL], orphanRemoval = true)
  @JoinColumn(name = "activity_id")
  var activityPay: ActivityPay? = null,

  val summary: String,

  val description: String,

  val startDate: LocalDate,

  var endDate: LocalDate? = null,

  var active: Boolean = false,

  val createdAt: LocalDateTime,

  val createdBy: String
)
