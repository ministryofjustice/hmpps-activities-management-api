package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode

class ReferenceCodeTest {
  private val studyAreaReferenceCode = ReferenceCode(
    domain = "STUDY_AREA",
    code = "ENGLA",
    description = "English Language",
    activeFlag = "Y",
    listSeq = 99,
    systemDataFlag = "N",
  )

  @Test
  fun `check isActive is true when activeFlag = 'Y'`() {
    assertThat(studyAreaReferenceCode.isActive()).isTrue
  }

  @Test
  fun `check isActive is false when activeFlag = 'N'`() {
    val inactiveReferenceCode = studyAreaReferenceCode.copy(activeFlag = "N")
    assertThat(inactiveReferenceCode.isActive()).isFalse
  }
}
