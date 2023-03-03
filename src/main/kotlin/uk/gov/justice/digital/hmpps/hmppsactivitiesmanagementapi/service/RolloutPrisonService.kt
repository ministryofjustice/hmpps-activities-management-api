package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transform

@Service
class RolloutPrisonService(private val repository: RolloutPrisonRepository) {

  fun getByPrisonCode(code: String) = transform(
    repository.findByCode(code) ?: throw EntityNotFoundException(code),
  )
}
