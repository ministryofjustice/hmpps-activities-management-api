package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata

import jakarta.validation.ValidationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform

@Service
@Transactional(readOnly = true)
class RolloutPrisonService(private val repository: RolloutPrisonRepository) {

  fun getByPrisonCode(code: String) = transform(
    repository.findByCode(code) ?: RolloutPrison(
      code = code,
      description = "Unknown prison",
      activitiesToBeRolledOut = false,
      appointmentsToBeRolledOut = false,
      maxDaysToExpiry = 21,
    ),
  )

  fun isActivitiesRolledOutAt(prisonCode: String): Boolean = repository.findByCode(prisonCode)?.isActivitiesRolledOut() == true

  fun getPrisonPlan(prisonCode: String): RolloutPrison = repository.findByCode(code = prisonCode) ?: throw ValidationException("no plan for $prisonCode")

  fun getAllPrisonPlans(): List<RolloutPrison> = repository.findAll()

  fun getRolloutPrisons() = repository.findAll().map { transform(it) }
}
