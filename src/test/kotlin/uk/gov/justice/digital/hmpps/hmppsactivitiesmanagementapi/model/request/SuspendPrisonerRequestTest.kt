package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import java.time.LocalDate

class SuspendPrisonerRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `status of SUSPENDED is valid`() {
    val request = SuspendPrisonerRequest(
      prisonerNumber = "G4793VF",
      allocationIds = listOf(1L),
      suspendFrom = LocalDate.now(),
      status = PrisonerStatus.SUSPENDED,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `status of SUSPENDED_WITH_PAY is valid`() {
    val request = SuspendPrisonerRequest(
      prisonerNumber = "G4793VF",
      allocationIds = listOf(1L),
      suspendFrom = LocalDate.now(),
      status = PrisonerStatus.SUSPENDED_WITH_PAY,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `any prisoner status not SUSPENDED or SUSPENDED_WITH_PAY is invalid`() {
    PrisonerStatus.allExcuding(PrisonerStatus.SUSPENDED, PrisonerStatus.SUSPENDED_WITH_PAY).forEach { status ->
      val request = SuspendPrisonerRequest(
        prisonerNumber = "G4793VF",
        allocationIds = listOf(1L),
        suspendFrom = LocalDate.now(),
        status = status,
      )
      assertSingleValidationError(validator.validate(request), "status", "Only 'SUSPENDED' or 'SUSPENDED_WITH_PAY' are allowed for status")
    }
  }

  private fun assertSingleValidationError(validate: MutableSet<ConstraintViolation<SuspendPrisonerRequest>>, propertyName: String, message: String) {
    assertThat(validate.size).isEqualTo(1)
    assertThat(validate.first().propertyPath.toString()).isEqualTo(propertyName)
    assertThat(validate.first().message).isEqualTo(message)
  }
}
