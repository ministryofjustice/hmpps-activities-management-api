package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "eligibility_rule")
data class EligibilityRule(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var eligibilityRuleId: Long = -1,

  val code: String,

  val description: String
)
