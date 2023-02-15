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
  fun `ethnicity field on generated Prisoner DTO type should be nullable`() {
    val field = Prisoner::class.declaredMembers.first { it.name == "ethnicity" }

    assertThat(field.returnType.isMarkedNullable).isTrue
  }

  @Test
  fun `youthOffender field on generated Prisoner DTO type should be nullable`() {
    val field = Prisoner::class.declaredMembers.first { it.name == "youthOffender" }

    assertThat(field.returnType.isMarkedNullable).isTrue
  }

  @Test
  fun `maritalStatus field on generated Prisoner DTO type should be nullable`() {
    val field = Prisoner::class.declaredMembers.first { it.name == "maritalStatus" }

    assertThat(field.returnType.isMarkedNullable).isTrue
  }

  @Test
  fun `religion field on generated Prisoner DTO type should be nullable`() {
    val field = Prisoner::class.declaredMembers.first { it.name == "religion" }

    assertThat(field.returnType.isMarkedNullable).isTrue
  }

  @Test
  fun `nationality field on generated Prisoner DTO type should be nullable`() {
    val field = Prisoner::class.declaredMembers.first { it.name == "nationality" }

    assertThat(field.returnType.isMarkedNullable).isTrue
  }

  @Test
  fun `mostSeriousOffence field on generated Prisoner DTO type should be nullable`() {
    val field = Prisoner::class.declaredMembers.first { it.name == "mostSeriousOffence" }

    assertThat(field.returnType.isMarkedNullable).isTrue
  }

  @Test
  fun `restrictedPatient field on generated Prisoner DTO type should be nullable`() {
    val field = Prisoner::class.declaredMembers.first { it.name == "restrictedPatient" }

    assertThat(field.returnType.isMarkedNullable).isTrue
  }
}
