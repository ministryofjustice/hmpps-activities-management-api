package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentParentCategory

@Repository
interface AppointmentParentCategoryRepository : JpaRepository<AppointmentParentCategory, Long>
