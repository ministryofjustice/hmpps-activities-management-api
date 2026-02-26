package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import kotlin.reflect.full.declaredMembers

/**
 * This test has been added to cover the fact some of generated prisoner search API types are incorrect.
 *
 * In this case fields generated as non-nullable are in fact nullable.
 */
class PrisonerSearchApiTypesTest {
  @Test
  fun `restrictedPatient field on generated Prisoner DTO type should be nullable`() {
    val field = Prisoner::class.declaredMembers.first { it.name == "restrictedPatient" }

    assertThat(field.returnType.isMarkedNullable).isTrue
  }
}
