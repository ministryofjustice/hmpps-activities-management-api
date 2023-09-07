package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentTier1
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.TIER_1_APPOINTMENT_TIER_ID

class AppointmentTierTest {
  @Test
  fun `entity to model mapping`() {
    val entity = appointmentTier1()
    entity.toModel() isEqualTo AppointmentTier(
      TIER_1_APPOINTMENT_TIER_ID,
      "Tier 1",
    )
  }
}
