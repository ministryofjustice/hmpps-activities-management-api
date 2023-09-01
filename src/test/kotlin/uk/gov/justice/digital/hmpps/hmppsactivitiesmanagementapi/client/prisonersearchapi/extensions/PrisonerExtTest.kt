package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
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
    temporarilyReleasedFromMoorland.isTemporarilyReleased() isBool true
    temporarilyReleasedFromMoorland.copy(confirmedReleaseDate = TimeSource.tomorrow()).isTemporarilyReleased() isBool true
  }

  @Test
  fun `is not temporarily released`() {
    temporarilyReleasedFromMoorland.copy(confirmedReleaseDate = TimeSource.today()).isTemporarilyReleased() isBool false
  }

  @Test
  fun `is permanently released`() {
    permanentlyReleasedFromMoorland.isPermanentlyReleased() isBool true
  }

  @Test
  fun `is not permanently released`() {
    permanentlyReleasedFromMoorland.copy(confirmedReleaseDate = TimeSource.tomorrow()).isPermanentlyReleased() isBool false
  }
}
