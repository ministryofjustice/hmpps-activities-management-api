package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeries

@Repository
interface AppointmentSeriesRepository :
  JpaRepository<AppointmentSeries, Long>,
  JpaSpecificationExecutor<AppointmentSeries>
