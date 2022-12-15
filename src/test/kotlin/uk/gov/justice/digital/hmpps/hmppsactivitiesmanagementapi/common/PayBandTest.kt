package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class PayBandTest {

  @Test
  fun `are equal`() {
    assertThat(PayBand.valueOf("a").toString()).isEqualTo("A")
    assertThat(PayBand.valueOf("B").toString()).isEqualTo("B")
    assertThat(PayBand.valueOf(" C").toString()).isEqualTo("C")
    assertThat(PayBand.valueOf("D ").toString()).isEqualTo("D")
    assertThat(PayBand.valueOf(" E ").toString()).isEqualTo("E")
  }

  @Test
  fun `cannot be blank or empty`() {
    assertThatThrownBy { PayBand.valueOf("") }.isInstanceOf(IllegalArgumentException::class.java)
    assertThatThrownBy { PayBand.valueOf(" ") }.isInstanceOf(IllegalArgumentException::class.java)
  }
}
