package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDate

class EarliestReleaseDateHelperTest {

  private lateinit var prisoner: Prisoner

  @BeforeEach
  fun setup() {
    prisoner = PrisonerSearchPrisonerFixture.instance().copy(
      releaseDate = LocalDate.of(2030, 4, 20),
      actualParoleDate = LocalDate.of(2036, 7, 17),
      tariffDate = LocalDate.of(20, 7, 17),
      legalStatus = Prisoner.LegalStatus.INDETERMINATE_SENTENCE,
    )
  }

  @Test
  fun `determineEarliestReleaseDate should return releaseDate if prisoner has one`() {
    val expectedReleaseDate = prisoner.releaseDate
    assertThat(determineEarliestReleaseDate(prisoner).releaseDate).isEqualTo(expectedReleaseDate)
    assertThat(determineEarliestReleaseDate(prisoner).isIndeterminateSentence).isTrue
  }

  @Test
  fun `determineEarliestReleaseDate should return actualParoleDate if prisoner has one and releaseDate is null`() {
    prisoner = prisoner.copy(releaseDate = null)

    val expectedReleaseDate = prisoner.actualParoleDate
    assertThat(determineEarliestReleaseDate(prisoner).releaseDate).isEqualTo(expectedReleaseDate)
    assertThat(determineEarliestReleaseDate(prisoner).isIndeterminateSentence).isTrue
  }

  @Test
  fun `determineEarliestReleaseDate should return tariffDate if prisoner has one and releaseDate and actualParoleDate are both null`() {
    prisoner = prisoner.copy(releaseDate = null, actualParoleDate = null)

    val expectedReleaseDate = prisoner.tariffDate
    assertThat(determineEarliestReleaseDate(prisoner).releaseDate).isEqualTo(expectedReleaseDate)
    assertThat(determineEarliestReleaseDate(prisoner).isTariffDate).isTrue
  }

  @Test
  fun `determineEarliestReleaseDate should return null if tariff date is present but legal status is not indeterminate sentence`() {
    prisoner = prisoner.copy(releaseDate = null, actualParoleDate = null, legalStatus = Prisoner.LegalStatus.DEAD)

    assertThat(determineEarliestReleaseDate(prisoner).releaseDate).isNull()
    assertThat(determineEarliestReleaseDate(prisoner).isTariffDate).isFalse
  }

  @Test
  fun `determineEarliestReleaseDate can return no releaseDate if isIndeterminateSentence`() {
    prisoner = prisoner.copy(releaseDate = null, actualParoleDate = null, tariffDate = null)

    assertThat(determineEarliestReleaseDate(prisoner).releaseDate).isNull()
    assertThat(determineEarliestReleaseDate(prisoner).isIndeterminateSentence).isTrue
  }

  @Test
  fun `determineEarliestReleaseDate returns no releaseDate if isConvictedUnsentenced`() {
    prisoner = prisoner.copy(
      releaseDate = null,
      legalStatus = Prisoner.LegalStatus.CONVICTED_UNSENTENCED,
    )

    assertThat(determineEarliestReleaseDate(prisoner).releaseDate).isNull()
    assertThat(determineEarliestReleaseDate(prisoner).isConvictedUnsentenced).isTrue
  }

  @Test
  fun `determineEarliestReleaseDate returns no releaseDate if isImmigrationDetainee`() {
    prisoner = prisoner.copy(
      releaseDate = null,
      legalStatus = Prisoner.LegalStatus.IMMIGRATION_DETAINEE,
    )

    assertThat(determineEarliestReleaseDate(prisoner).releaseDate).isNull()
    assertThat(determineEarliestReleaseDate(prisoner).isImmigrationDetainee).isTrue
  }

  @Test
  fun `determineEarliestReleaseDate returns no releaseDate if isRemand`() {
    prisoner = prisoner.copy(
      releaseDate = null,
      legalStatus = Prisoner.LegalStatus.REMAND,
    )

    assertThat(determineEarliestReleaseDate(prisoner).releaseDate).isNull()
    assertThat(determineEarliestReleaseDate(prisoner).isRemand).isTrue
  }
}
