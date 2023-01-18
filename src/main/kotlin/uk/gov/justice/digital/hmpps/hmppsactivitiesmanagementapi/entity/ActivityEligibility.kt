package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "activity_eligibility")
data class ActivityEligibility(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityEligibilityId: Long = -1,

  @OneToOne
  @JoinColumn(name = "eligibility_rule_id", nullable = false)
  val eligibilityRule: EligibilityRule,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity
)
