package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import java.time.LocalDate

class WaitingListSearchRequestTest {
  private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

  @Test
  fun `valid request`() {
    val request = WaitingListSearchRequest(
      applicationDateFrom = LocalDate.parse("2023-01-01"),
      applicationDateTo = LocalDate.parse("2023-01-31"),
      activityId = 2,
      prisonerNumbers = listOf("ABC1234"),
      status = listOf(WaitingListStatus.APPROVED),
    )
    assertThat(validator.validate(request)).isEmpty()
  }
}
