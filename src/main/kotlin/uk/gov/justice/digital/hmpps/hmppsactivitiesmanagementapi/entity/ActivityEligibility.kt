package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EligibilityRule

@Entity
@Table(name = "activity_eligibility")
data class ActivityEligibility(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val activityEligibilityId: Long = 0,

  @OneToOne
  @JoinColumn(name = "eligibility_rule_id", nullable = false)
  val eligibilityRule: EligibilityRule,

  @ManyToOne
  @JoinColumn(name = "activity_id", nullable = false)
  val activity: Activity,
)
