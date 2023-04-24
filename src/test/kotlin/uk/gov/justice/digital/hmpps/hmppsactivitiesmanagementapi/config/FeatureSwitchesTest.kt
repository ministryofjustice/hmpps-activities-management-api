package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
class FeatureSwitchesTest {

  @TestPropertySource(
    properties = [
      "feature.events.sns.enabled=true",
      "feature.event.activities.activity-schedule.created=true",
      "feature.event.activities.prisoner.allocated=true",
      "feature.event.appointments.appointment-instance.created=true",
      "feature.event.prison-offender-events.prisoner.received=true",
      "feature.event.prison-offender-events.prisoner.released=true",
    ],
  )
  @Nested
  @DisplayName("Features are enabled when set")
  inner class EnabledFeatures(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `features are enabled`() {
      assertThat(featureSwitches.isEnabled(Feature.OUTBOUND_EVENTS_ENABLED)).isTrue
      assertThat(featureSwitches.isEnabled(OutboundEvent.ACTIVITY_SCHEDULE_CREATED)).isTrue
      assertThat(featureSwitches.isEnabled(OutboundEvent.PRISONER_ALLOCATED)).isTrue
      assertThat(featureSwitches.isEnabled(OutboundEvent.APPOINTMENT_INSTANCE_CREATED)).isTrue
      assertThat(featureSwitches.isEnabled(InboundEventType.OFFENDER_RECEIVED)).isTrue
      assertThat(featureSwitches.isEnabled(InboundEventType.OFFENDER_RELEASED)).isTrue
    }
  }

  @TestPropertySource(
    properties = [
      "feature.events.sns.enabled=true",
      "feature.event.activities.prisoner.attendance-created=true",
      "feature.event.activities.prisoner.attendance-amended=true",
    ],
  )
  @Nested
  @DisplayName("Attendance feature switches are enabled when set")
  inner class AttendanceEventsEnabledFeatures(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `features are enabled`() {
      assertThat(featureSwitches.isEnabled(Feature.OUTBOUND_EVENTS_ENABLED)).isTrue
      assertThat(featureSwitches.isEnabled(OutboundEvent.PRISONER_ATTENDANCE_CREATED)).isTrue
      assertThat(featureSwitches.isEnabled(OutboundEvent.PRISONER_ATTENDANCE_AMENDED)).isTrue
    }
  }

  @Nested
  @DisplayName("Features are disabled by default")
  inner class DisabledFeatures(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `features are disabled by default`() {
      assertThat(featureSwitches.isEnabled(Feature.OUTBOUND_EVENTS_ENABLED)).isFalse
      assertThat(featureSwitches.isEnabled(OutboundEvent.ACTIVITY_SCHEDULE_CREATED)).isFalse
      assertThat(featureSwitches.isEnabled(OutboundEvent.PRISONER_ALLOCATED)).isFalse
      assertThat(featureSwitches.isEnabled(OutboundEvent.APPOINTMENT_INSTANCE_CREATED)).isFalse
      assertThat(featureSwitches.isEnabled(InboundEventType.OFFENDER_RECEIVED)).isFalse
      assertThat(featureSwitches.isEnabled(InboundEventType.OFFENDER_RELEASED)).isFalse
    }
  }

  @Nested
  @DisplayName("Features can be defaulted when not present")
  inner class DefaultedFeatures(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `features are defaulted`() {
      assertThat(featureSwitches.isEnabled(Feature.OUTBOUND_EVENTS_ENABLED, true)).isTrue
      assertThat(featureSwitches.isEnabled(OutboundEvent.ACTIVITY_SCHEDULE_CREATED, true)).isTrue
      assertThat(featureSwitches.isEnabled(OutboundEvent.PRISONER_ALLOCATED, true)).isTrue
      assertThat(featureSwitches.isEnabled(OutboundEvent.APPOINTMENT_INSTANCE_CREATED, true)).isTrue
      assertThat(featureSwitches.isEnabled(InboundEventType.OFFENDER_RECEIVED, true)).isTrue
      assertThat(featureSwitches.isEnabled(InboundEventType.OFFENDER_RELEASED, true)).isTrue
    }
  }
}
