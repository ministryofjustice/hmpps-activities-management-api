package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeInMoorlandPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeInPentonvillePrisoner
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
  fun `is active in prison`() {
    activeInMoorlandPrisoner.isActiveAtPrison(MOORLAND_PRISON_CODE) isBool true
    activeInMoorlandPrisoner.isActiveAtPrison(PENTONVILLE_PRISON_CODE) isBool false

    activeInPentonvillePrisoner.isActiveAtPrison(PENTONVILLE_PRISON_CODE) isBool true
    activeInPentonvillePrisoner.isActiveAtPrison(MOORLAND_PRISON_CODE) isBool false
  }

  @Test
  fun `is active at prison`() {
    activeInMoorlandPrisoner.isActiveAtPrison(MOORLAND_PRISON_CODE) isBool true
    activeInMoorlandPrisoner.isActiveAtPrison(PENTONVILLE_PRISON_CODE) isBool false

    activeOutMoorlandPrisoner.isActiveAtPrison(MOORLAND_PRISON_CODE) isBool true
    activeOutMoorlandPrisoner.isActiveAtPrison(PENTONVILLE_PRISON_CODE) isBool false
  }

  @Test
  fun `is at different location`() {
    activeInMoorlandPrisoner.isAtDifferentLocationTo(PENTONVILLE_PRISON_CODE) isBool true
    activeInMoorlandPrisoner.isAtDifferentLocationTo(PENTONVILLE_PRISON_CODE) isBool true
    activeInMoorlandPrisoner.isAtDifferentLocationTo(MOORLAND_PRISON_CODE) isBool false
  }
}
