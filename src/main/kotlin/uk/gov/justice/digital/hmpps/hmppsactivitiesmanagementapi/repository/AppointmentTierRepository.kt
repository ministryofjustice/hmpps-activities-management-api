package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentTier

const val TIER_1_APPOINTMENT_TIER_ID = 1L
const val TIER_2_APPOINTMENT_TIER_ID = 2L
const val NO_TIER_APPOINTMENT_TIER_ID = 3L
const val NOT_SPECIFIED_APPOINTMENT_TIER_ID = 4L

@Repository
interface AppointmentTierRepository : JpaRepository<AppointmentTier, Long>
