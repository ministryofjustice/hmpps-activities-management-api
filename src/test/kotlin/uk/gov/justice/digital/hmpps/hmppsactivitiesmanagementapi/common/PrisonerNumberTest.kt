package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PrisonerNumberTest {

  @Test
  fun `are equal`() {
    assertThat(PrisonerNumber.valueOf("a").toString()).isEqualTo("A")
    assertThat(PrisonerNumber.valueOf("B").toString()).isEqualTo("B")
    assertThat(PrisonerNumber.valueOf(" C").toString()).isEqualTo("C")
    assertThat(PrisonerNumber.valueOf("D ").toString()).isEqualTo("D")
    assertThat(PrisonerNumber.valueOf(" E ").toString()).isEqualTo("E")
  }

  @Test
  fun `cannot be blank or empty`() {
    assertThatThrownBy { PrisonerNumber.valueOf("") }.isInstanceOf(IllegalArgumentException::class.java)
    assertThatThrownBy { PrisonerNumber.valueOf(" ") }.isInstanceOf(IllegalArgumentException::class.java)
  }
}
