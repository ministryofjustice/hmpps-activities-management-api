package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import java.time.LocalDateTime

class LocalDateTimeExtTest {

  @Test
  fun `to ISO format`() {
    LocalDateTime.of(2024, 1, 1, 10, 0).toIsoDateTime() isEqualTo "2024-01-01T10:00:00"
  }

  @Test
  fun `to medium format style`() {
    LocalDateTime.of(2024, 1, 1, 10, 0).toMediumFormatStyle() isEqualTo "1 Jan 2024, 10:00:00"
  }
}
