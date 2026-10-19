package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSeries
import java.time.LocalDate
import java.util.*

@Repository
interface AppointmentSeriesRepository :
  JpaRepository<AppointmentSeries, Long>,
  JpaSpecificationExecutor<AppointmentSeries> {

  @Query(
    """
      select distinct s.appointmentSeriesId from AppointmentSeries s, Appointment a
      where a.appointmentSeries = s and s.prisonCode = :prisonCode
      and a.startDate > :today and a.isDeleted = false
      and (s.dpsLocationId = :locationId or a.dpsLocationId = :locationId)
    """,
  )
  fun findSeriesIdsForLocationRemap(prisonCode: String, locationId: UUID, today: LocalDate): List<Long>

  @Query(
    """
    select distinct a.internalLocationId 
    from AppointmentSeries a 
    where a.internalLocationId is not null
    and a.dpsLocationId is null
  """,
  )
  fun findNomisLocationsIds(): List<Int>

  @Query(
    value = """
    update AppointmentSeries a 
    set a.dpsLocationId = :dpsLocationId
    where a.internalLocationId = :internalLocationId
  """,
  )
  @Modifying
  fun updateLocationDetails(internalLocationId: Int, dpsLocationId: UUID)
}
