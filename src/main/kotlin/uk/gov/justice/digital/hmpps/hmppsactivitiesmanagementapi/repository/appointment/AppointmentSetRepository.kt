package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentSet
import java.util.UUID

@Repository
interface AppointmentSetRepository : JpaRepository<AppointmentSet, Long> {

  @Query(
    """
    select distinct a.internalLocationId 
    from AppointmentSet a 
    where a.internalLocationId is not null
    and a.dpsLocationId is null
  """,
  )
  fun findNomisLocationsIds(): List<Int>

  @Query(
    value = """
    update AppointmentSet a 
    set a.dpsLocationId = :dpsLocationId
    where a.internalLocationId = :internalLocationId
  """,
  )
  @Modifying
  fun updateLocationDetails(internalLocationId: Int, dpsLocationId: UUID)
}
