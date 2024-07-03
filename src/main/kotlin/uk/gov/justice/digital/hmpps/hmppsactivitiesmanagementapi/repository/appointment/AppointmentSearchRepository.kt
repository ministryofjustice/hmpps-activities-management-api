package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment

import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSearch
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ReadOnlyRepository

@Repository
interface AppointmentSearchRepository :
  ReadOnlyRepository<AppointmentSearch, Long>,
  JpaSpecificationExecutor<AppointmentSearch>
