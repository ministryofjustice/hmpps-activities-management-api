package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.SubjectAccessRequestContent
import java.time.LocalDate

@Service
class SubjectAccessRequestService {
  fun getContentFor(prisonerNumber: String, fromDate: LocalDate?, toDate: LocalDate?): SubjectAccessRequestContent? = TODO("SAA-1528 - work in progress")
}
