package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata

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
    ),
  )

  fun getRolloutPrisons() = repository.findAll().map { transform(it) }
}
