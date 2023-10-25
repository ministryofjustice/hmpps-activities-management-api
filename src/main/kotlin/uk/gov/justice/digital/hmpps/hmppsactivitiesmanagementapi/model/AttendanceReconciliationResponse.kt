package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "A list of paid attendance counts for each booking in the prison on the date")
data class AttendanceReconciliationResponse(
  @Schema(description = "The prison code", example = "BXI")
  val prisonCode: String,
  @Schema(description = "The date to check", example = "2023-10-25")
  val date: LocalDate,
  @Schema(description = "A list of bookings and the number of paid attendances for each", example = """{ [ "bookingId": 12345, "count": 2 ] }""")
  val bookings: List<BookingCount>,
)
