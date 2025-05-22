package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AdvanceAttendanceCreateRequestTest : ValidatorBase<AdvanceAttendanceCreateRequest>() {

  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid create request`() {
    val request = AdvanceAttendanceCreateRequest(
      scheduleInstanceId = 1,
      prisonerNumber = "A1111AA",
      issuePayment = false,
    )
    assertThat(validator.validate(request)).isEmpty()
  }

  @Test
  fun `scheduled instance id must not be null`() {
    val request = AdvanceAttendanceCreateRequest(
      scheduleInstanceId = null,
      prisonerNumber = "A1111AA",
      issuePayment = true,
    )
    assertThat(
      validator.validate(request),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("scheduleInstanceId")
      assertThat(it.message).isEqualTo("Schedule instance id must be supplied")
    }
  }

  @Test
  fun `prisoner number must not be null`() {
    val request = AdvanceAttendanceCreateRequest(
      scheduleInstanceId = 123,
      prisonerNumber = null,
      issuePayment = false,
    )
    assertThat(
      validator.validate(request),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("prisonerNumber")
      assertThat(it.message).isEqualTo("Prisoner number must be supplied")
    }
  }

  @Test
  fun `prisoner number must not be blank`() {
    val request = AdvanceAttendanceCreateRequest(
      scheduleInstanceId = 123,
      prisonerNumber = "",
      issuePayment = false,
    )
    assertThat(
      validator.validate(request),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("prisonerNumber")
      assertThat(it.message).isEqualTo("Prisoner number must be supplied")
    }
  }

  @Test
  fun `issue payment must not be null`() {
    val request = AdvanceAttendanceCreateRequest(
      scheduleInstanceId = 123,
      prisonerNumber = "A1111AA",
      issuePayment = null,
    )
    assertThat(
      validator.validate(request),
    ).satisfiesOnlyOnce {
      assertThat(it.propertyPath.toString()).isEqualTo("issuePayment")
      assertThat(it.message).isEqualTo("Issue payment must be supplied")
    }
  }
}
