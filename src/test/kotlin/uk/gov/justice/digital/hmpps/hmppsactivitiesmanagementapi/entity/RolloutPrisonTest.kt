package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RolloutPrisonTest {

  @Test
  fun `isActivitiesRolledOut returns false when activitiesToBeRolledOut is false`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", false, null, false, null)
    assertThat(rolloutPrison.isActivitiesRolledOut()).isFalse
  }

  @Test
  fun `isActivitiesRolledOut returns false when activitiesToBeRolledOut is true but activitiesRolloutDate is null`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", true, null, false, null)
    assertThat(rolloutPrison.isActivitiesRolledOut()).isFalse
  }

  @Test
  fun `isActivitiesRolledOut returns false when activitiesToBeRolledOut is true but activitiesRolloutDate is in the future`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", true, LocalDate.of(2600, 1, 1), false, null)
    assertThat(rolloutPrison.isActivitiesRolledOut()).isFalse
  }

  @Test
  fun `isActivitiesRolledOut returns true when activitiesToBeRolledOut is true and activitiesRolloutDate is in the past`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", true, LocalDate.of(2022, 1, 1), false, null)
    assertThat(rolloutPrison.isActivitiesRolledOut()).isTrue
  }

  @Test
  fun `isActivitiesRolledOut returns true when activitiesToBeRolledOut is true and activitiesRolloutDate is in the present`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", true, LocalDate.now(), false, null)
    assertThat(rolloutPrison.isActivitiesRolledOut()).isTrue
  }

  @Test
  fun `isAppointmentsRolledOut returns false when appointmentsToBeRolledOut is false`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", false, null, false, null)
    assertThat(rolloutPrison.isAppointmentsRolledOut()).isFalse
  }

  @Test
  fun `isAppointmentsRolledOut returns false when appointmentsToBeRolledOut is true but appointmentsRolloutDate is null`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", false, null, true, null)
    assertThat(rolloutPrison.isAppointmentsRolledOut()).isFalse
  }

  @Test
  fun `isAppointmentsRolledOut returns false when appointmentsToBeRolledOut is true but appointmentsRolloutDate is in the future`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", false, null, true, LocalDate.of(2600, 1, 1))
    assertThat(rolloutPrison.isAppointmentsRolledOut()).isFalse
  }

  @Test
  fun `isAppointmentsRolledOut returns true when appointmentsToBeRolledOut is true and appointmentsRolloutDate is in the past`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", false, null, true, LocalDate.of(2022, 1, 1))
    assertThat(rolloutPrison.isAppointmentsRolledOut()).isTrue
  }

  @Test
  fun `isAppointmentsRolledOut returns true when appointmentsToBeRolledOut is true and appointmentsRolloutDate is in the present`() {
    val rolloutPrison = RolloutPrison(1, "PBI", "Some Desc", false, null, true, LocalDate.now())
    assertThat(rolloutPrison.isAppointmentsRolledOut()).isTrue
  }
}
