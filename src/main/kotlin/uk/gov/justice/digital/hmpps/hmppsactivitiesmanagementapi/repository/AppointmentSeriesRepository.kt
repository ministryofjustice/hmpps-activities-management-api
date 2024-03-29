package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries

@Repository
interface AppointmentSeriesRepository :
  JpaRepository<AppointmentSeries, Long>,
  JpaSpecificationExecutor<AppointmentSeries>
