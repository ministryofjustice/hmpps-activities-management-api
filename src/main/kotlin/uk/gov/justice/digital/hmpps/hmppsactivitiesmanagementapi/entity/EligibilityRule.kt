package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "eligibility_rule")
data class EligibilityRule(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var eligibilityRuleId: Long = -1,

  val code: String,

  val description: String
)
