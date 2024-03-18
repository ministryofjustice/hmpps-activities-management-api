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
      "feature.offender.merge.enabled=true",
      "feature.migrate.split.regime.enabled=true",
      "feature.audit.service.hmpps.enabled=true",
      "feature.audit.service.local.enabled=true",
      "feature.events.sns.enabled=true",
      "feature.event.activities.activity-schedule.created=true",
      "feature.event.activities.prisoner.allocated=true",
      "feature.event.appointments.appointment-instance.created=true",
      "feature.event.prison-offender-events.prisoner.received=true",
      "feature.event.prison-offender-events.prisoner.released=true",
      "feature.event.activities.activity-schedule.amended=true",
      "feature.event.activities.scheduled-instance.amended=true",
      "feature.event.activities.prisoner.allocation-amended=true",
      "feature.event.activities.prisoner.attendance-created=true",
      "feature.event.activities.prisoner.attendance-amended=true",
      "feature.event.activities.prisoner.attendance-expired=true",
      "feature.event.appointments.appointment-instance.updated=true",
      "feature.event.appointments.appointment-instance.deleted=true",
      "feature.event.appointments.appointment-instance.cancelled=true",
      "feature.event.incentives.iep-review.inserted=true",
      "feature.event.incentives.iep-review.updated=true",
      "feature.event.incentives.iep-review.deleted=true",
      "feature.event.prison-offender-events.prisoner.cell.move=true",
      "feature.event.prison-offender-events.prisoner.non-association-detail.changed=true",
      "feature.event.prison-offender-events.prisoner.activities-changed=true",
      "feature.event.prison-offender-events.prisoner.appointments-changed=true",
      "feature.event.prison-offender-events.prisoner.merged=true",
      "feature.event.prisoner-offender-search.prisoner.alerts-updated=true",
    ],
  )
  @Nested
  @DisplayName("Features are enabled when set")
  inner class EnabledFeatures(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `features are enabled`() {
      Feature.entries.forEach {
        assertThat(featureSwitches.isEnabled(it)).withFailMessage("${it.label} not enabled").isTrue
      }

      OutboundEvent.entries.forEach {
        assertThat(featureSwitches.isEnabled(it)).withFailMessage("${it.eventType} not enabled").isTrue
      }

      InboundEventType.entries.forEach {
        assertThat(featureSwitches.isEnabled(it)).withFailMessage("${it.eventType} not enabled").isTrue
      }
    }
  }

  @Nested
  @DisplayName("Features are disabled by default")
  inner class DisabledFeatures(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `features are disabled by default`() {
      Feature.entries.forEach {
        assertThat(featureSwitches.isEnabled(it)).withFailMessage("${it.label} enabled").isFalse
      }

      OutboundEvent.entries.forEach {
        assertThat(featureSwitches.isEnabled(it)).withFailMessage("${it.eventType} enabled").isFalse
      }

      InboundEventType.entries.forEach {
        assertThat(featureSwitches.isEnabled(it)).withFailMessage("${it.eventType} enabled").isFalse
      }
    }
  }

  @Nested
  @DisplayName("Features can be defaulted when not present")
  inner class DefaultedFeatures(@Autowired val featureSwitches: FeatureSwitches) {
    @Test
    fun `different feature types can be defaulted `() {
      assertThat(featureSwitches.isEnabled(Feature.OUTBOUND_EVENTS_ENABLED, true)).isTrue
      assertThat(featureSwitches.isEnabled(OutboundEvent.ACTIVITY_SCHEDULE_CREATED, true)).isTrue
      assertThat(featureSwitches.isEnabled(InboundEventType.OFFENDER_RELEASED, true)).isTrue
    }
  }
}
