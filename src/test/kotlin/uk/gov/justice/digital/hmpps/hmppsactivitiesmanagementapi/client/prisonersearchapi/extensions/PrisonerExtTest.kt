package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeInMoorlandPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeOutMoorlandPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.permanentlyReleasedPrisonerToday
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.temporarilyReleasedFromMoorland

class PrisonerExtTest {

  @Test
  fun `is temporarily released`() {
    temporarilyReleasedFromMoorland.status isEqualTo "ACTIVE OUT"
    temporarilyReleasedFromMoorland.isActiveOut() isBool true
    temporarilyReleasedFromMoorland.isTemporarilyReleased() isBool true
    temporarilyReleasedFromMoorland.copy(confirmedReleaseDate = TimeSource.tomorrow()).isTemporarilyReleased() isBool true
  }

  @Test
  fun `is not temporarily released`() {
    temporarilyReleasedFromMoorland.copy(confirmedReleaseDate = TimeSource.today()).isTemporarilyReleased() isBool false
  }

  @Test
  fun `is permanently released`() {
    permanentlyReleasedPrisonerToday.status isEqualTo "INACTIVE OUT"
    permanentlyReleasedPrisonerToday.isInactiveOut() isBool true
    permanentlyReleasedPrisonerToday.isPermanentlyReleased() isBool true
  }

  @Test
  fun `is not permanently released`() {
    permanentlyReleasedPrisonerToday.copy(confirmedReleaseDate = TimeSource.tomorrow()).isPermanentlyReleased() isBool false
  }

  @Test
  fun `is restricted patient`() {
    permanentlyReleasedPrisonerToday.copy(restrictedPatient = true).isRestrictedPatient() isBool true
    permanentlyReleasedPrisonerToday.copy(restrictedPatient = false).isRestrictedPatient() isBool false
  }

  @Test
  fun `is active in prisoner`() {
    activeInMoorlandPrisoner.status isEqualTo "ACTIVE IN"
    activeInMoorlandPrisoner.isActiveIn() isBool true
  }

  @Test
  fun `is active at prison`() {
    activeInMoorlandPrisoner.isActiveAtPrison(moorlandPrisonCode) isBool true
    activeInMoorlandPrisoner.isActiveAtPrison(pentonvillePrisonCode) isBool false

    activeOutMoorlandPrisoner.isActiveAtPrison(moorlandPrisonCode) isBool true
    activeOutMoorlandPrisoner.isActiveAtPrison(pentonvillePrisonCode) isBool false
  }

  @Test
  fun `is at different location`() {
    activeInMoorlandPrisoner.isAtDifferentLocationTo(pentonvillePrisonCode) isBool true
    activeInMoorlandPrisoner.isAtDifferentLocationTo(pentonvillePrisonCode) isBool true
    activeInMoorlandPrisoner.isAtDifferentLocationTo(moorlandPrisonCode) isBool false
  }
}
