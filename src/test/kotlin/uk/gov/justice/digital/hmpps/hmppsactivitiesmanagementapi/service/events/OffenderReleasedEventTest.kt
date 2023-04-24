package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode

class OffenderReleasedEventTest {
  @Test
  fun `release event is temporary`() {
    assertThat(releaseEvent("TEMPORARY_ABSENCE_RELEASE").isTemporary()).isTrue
    assertThat(releaseEvent("RELEASED_TO_HOSPITAL").isTemporary()).isTrue
    assertThat(releaseEvent("SENT_TO_COURT").isTemporary()).isTrue
  }

  @Test
  fun `temporary release event is not permanent`() {
    assertThat(releaseEvent("TEMPORARY_ABSENCE_RELEASE").isPermanent()).isFalse
    assertThat(releaseEvent("RELEASED_TO_HOSPITAL").isPermanent()).isFalse
    assertThat(releaseEvent("SENT_TO_COURT").isPermanent()).isFalse
  }

  @Test
  fun `release event is permanent`() {
    assertThat(releaseEvent("RELEASED").isPermanent()).isTrue
    assertThat(releaseEvent("TRANSFERRED").isPermanent()).isTrue

    assertThat(releaseEvent("RELEASED").isTemporary()).isFalse
    assertThat(releaseEvent("TRANSFERRED").isTemporary()).isFalse
  }

  @Test
  fun `permanent release event is not temporary`() {
    assertThat(releaseEvent("RELEASED").isTemporary()).isFalse
    assertThat(releaseEvent("TRANSFERRED").isTemporary()).isFalse
  }

  @Test
  fun `release event is not temporary or permanent`() {
    assertThat(releaseEvent("UNKNOWN").isTemporary()).isFalse
    assertThat(releaseEvent("UNKNOWN").isPermanent()).isFalse
  }

  private fun releaseEvent(reason: String) =
    OffenderReleasedEvent(ReleaseInformation("123456", reason, moorlandPrisonCode))
}
