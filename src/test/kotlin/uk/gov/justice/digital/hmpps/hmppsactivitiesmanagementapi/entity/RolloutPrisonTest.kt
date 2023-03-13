package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class RolloutPrisonTest {

  @Test
  fun `isAppointmentsEnabled returns false when active is false and appointmentsDataSource is PRISON_API`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", false, null, AppointmentsDataSource.PRISON_API)
    assertThat(rolloutPrison.isAppointmentsEnabled()).isFalse
  }

  @Test
  fun `isAppointmentsEnabled returns false when active is false and appointmentsDataSource is ACTIVITIES_SERVICE`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", false, null, AppointmentsDataSource.ACTIVITIES_SERVICE)
    assertThat(rolloutPrison.isAppointmentsEnabled()).isFalse
  }

  @Test
  fun `isAppointmentsEnabled returns false when active is true and appointmentsDataSource is PRISON_API`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", true, null, AppointmentsDataSource.PRISON_API)
    assertThat(rolloutPrison.isAppointmentsEnabled()).isFalse
  }

  @Test
  fun `isAppointmentsEnabled returns true when active is true and appointmentsDataSource is ACTIVITIES_SERVICE`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", true, null, AppointmentsDataSource.ACTIVITIES_SERVICE)
    assertThat(rolloutPrison.isAppointmentsEnabled()).isTrue
  }
}
