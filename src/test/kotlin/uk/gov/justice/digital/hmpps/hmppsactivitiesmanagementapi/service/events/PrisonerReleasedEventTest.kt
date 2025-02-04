package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool

class PrisonerReleasedEventTest {
  @Test
  fun `release event is temporary`() {
    releaseEvent("TEMPORARY_ABSENCE_RELEASE").isTemporary() isBool true
    releaseEvent("SENT_TO_COURT").isTemporary() isBool true
  }

  @Test
  fun `temporary release event is not permanent`() {
    releaseEvent("TEMPORARY_ABSENCE_RELEASE").isPermanent() isBool false
    releaseEvent("SENT_TO_COURT").isPermanent() isBool false
  }

  @Test
  fun `release event is permanent`() {
    releaseEvent("RELEASED").isPermanent() isBool true
    releaseEvent("RELEASED_TO_HOSPITAL").isPermanent() isBool true
  }

  @Test
  fun `permanent release event is not temporary`() {
    releaseEvent("RELEASED").isTemporary() isBool false
    releaseEvent("RELEASED_TO_HOSPITAL").isTemporary() isBool false
  }

  @Test
  fun `release event is not temporary or permanent`() {
    releaseEvent("UNKNOWN").isTemporary() isBool false
    releaseEvent("UNKNOWN").isPermanent() isBool false
  }

  private fun releaseEvent(reason: String) = PrisonerReleasedEvent(ReleaseInformation("123456", reason, MOORLAND_PRISON_CODE))
}
