package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SubjectAccessRequestContent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.SarRepository
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SarAllocation as ModelSarAllocation

@Service
class SubjectAccessRequestService(private val repository: SarRepository) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getContentFor(prisonerNumber: String, fromDate: LocalDate?, toDate: LocalDate?): SubjectAccessRequestContent? {
    val from = fromDate ?: LocalDate.now()
    val to = toDate ?: LocalDate.now()

    log.info("before")
    val allocations = repository.findAllocationsBy(prisonerNumber, from, to).also { log.info(it.joinToString(",")) }
    log.info("after")

    return if (allocations.isEmpty()) {
      null
    } else {
      SubjectAccessRequestContent(
        prisonerNumber = prisonerNumber,
        fromDate = from,
        toDate = to,
        allocations = allocations.map(::ModelSarAllocation),
      )
    }
  }
}
