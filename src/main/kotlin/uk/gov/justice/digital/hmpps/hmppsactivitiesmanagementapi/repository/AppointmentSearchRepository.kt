package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSearch

@Repository
interface AppointmentSearchRepository : 
  ReadOnlyRepository<AppointmentSearch, Long>,
  JpaSpecificationExecutor<AppointmentSearch>
