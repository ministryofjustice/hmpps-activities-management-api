package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform

@Service
class RolloutPrisonService(private val repository: RolloutPrisonRepository) {

  fun getByPrisonCode(code: String) = transform(
    repository.findByCode(code) ?: RolloutPrison(
      code = code,
      description = "Unknown prison",
      activitiesToBeRolledOut = false,
      appointmentsToBeRolledOut = false,
    ),
  )
}
