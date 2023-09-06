package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentHost

const val PRISON_STAFF_APPOINTMENT_HOST_ID = 1L
const val PRISONER_OR_PRISONERS_APPOINTMENT_HOST_ID = 2L
const val EXTERNAL_PROVIDER_APPOINTMENT_HOST_ID = 3L
const val SOMEONE_ELSE_APPOINTMENT_HOST_ID = 4L

@Repository
interface AppointmentHostRepository : JpaRepository<AppointmentHost, Long>
