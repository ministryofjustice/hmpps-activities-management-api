package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import java.time.LocalDate

class PrisonerExtTest {

  private val temporarilyReleasedFromMoorland = Prisoner(
    prisonerNumber = "1",
    firstName = "Freddie",
    lastName = "Bloggs",
    dateOfBirth = LocalDate.EPOCH,
    gender = "Female",
    status = "ACTIVE OUT",
    prisonId = moorlandPrisonCode,
    confirmedReleaseDate = null,
  )

  private val permanentlyReleasedFromMoorland = Prisoner(
    prisonerNumber = "1",
    firstName = "Freddie",
    lastName = "Bloggs",
    dateOfBirth = LocalDate.EPOCH,
    gender = "Female",
    status = "INACTIVE OUT",
    prisonId = moorlandPrisonCode,
    confirmedReleaseDate = TimeSource.today(),
    lastMovementTypeCode = "REL",
  )

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
    permanentlyReleasedFromMoorland.status isEqualTo "INACTIVE OUT"
    permanentlyReleasedFromMoorland.isInactiveOut() isBool true
    permanentlyReleasedFromMoorland.isPermanentlyReleased() isBool true
  }

  @Test
  fun `is not permanently released`() {
    permanentlyReleasedFromMoorland.copy(confirmedReleaseDate = TimeSource.tomorrow()).isPermanentlyReleased() isBool false
  }

  @Test
  fun `is restricted patient`() {
    permanentlyReleasedFromMoorland.copy(restrictedPatient = true).isRestrictedPatient() isBool true
    permanentlyReleasedFromMoorland.copy(restrictedPatient = false).isRestrictedPatient() isBool false
  }

  @Test
  fun `is active in prisoner`() {
    val activeInPrisoner = Prisoner(
      prisonerNumber = "1",
      firstName = "Freddie",
      lastName = "Bloggs",
      dateOfBirth = LocalDate.EPOCH,
      gender = "Female",
      status = "ACTIVE IN",
      prisonId = moorlandPrisonCode,
      confirmedReleaseDate = null,
    )

    activeInPrisoner.status isEqualTo "ACTIVE IN"
    activeInPrisoner.isActiveIn() isBool true
  }

  @Test
  fun `is at different location`() {
    val activeInPrisoner = Prisoner(
      prisonerNumber = "1",
      firstName = "Freddie",
      lastName = "Bloggs",
      dateOfBirth = LocalDate.EPOCH,
      gender = "Female",
      status = "ACTIVE IN",
      prisonId = moorlandPrisonCode,
      confirmedReleaseDate = null,
    )

    activeInPrisoner.isAtDifferentLocationTo(pentonvillePrisonCode) isBool true
    activeInPrisoner.copy(prisonId = null).isAtDifferentLocationTo(pentonvillePrisonCode) isBool true
    activeInPrisoner.isAtDifferentLocationTo(moorlandPrisonCode) isBool false
  }
}
