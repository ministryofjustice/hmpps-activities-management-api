package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import kotlin.reflect.full.declaredMembers

/**
 * This test has been added to cover the fact some of generated prison API types are incorrect.
 *
 * In this case fields generated as non-nullable are in fact nullable.
 */
class PrisonApiTypesTest {

  @Test
  fun `comment field on generated Alert DTO type should be nullable`() {
    val commentField = Alert::class.declaredMembers.first { it.name == "comment" }

    assertThat(commentField.returnType.isMarkedNullable).isTrue
  }

  @Test
  fun `classification field on generated Assessment DTO type should be nullable`() {
    val classificationField = Assessment::class.declaredMembers.first { it.name == "classification" }

    assertThat(classificationField.returnType.isMarkedNullable).isTrue
  }

  @Test
  fun `lockDate field on generated User Detail DTO type should be nullable`() {
    val field = UserDetail::class.declaredMembers.first { it.name == "lockDate" }

    assertThat(field.returnType.isMarkedNullable).isTrue
  }

  @Test
  fun `domain field on generated Reference Code DTO type should be nullable`() {
    val field = ReferenceCode::class.declaredMembers.first { it.name == "domain" }

    assertThat(field.returnType.isMarkedNullable).isTrue
  }
}
