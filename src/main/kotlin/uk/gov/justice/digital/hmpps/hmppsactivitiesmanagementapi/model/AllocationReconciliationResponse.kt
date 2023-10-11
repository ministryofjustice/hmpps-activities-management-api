package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A list of allocation counts for each booking in the prison")
data class AllocationReconciliationResponse(
  @Schema(description = "The prison code", example = "BXI")
  val prisonCode: String,
  @Schema(description = "A list of bookings and the number of active allocations for each", example = """{ [ "bookingId": 12345, "count": 2 ] }""")
  val bookings: List<BookingCount>,
)

@Schema(description = "The count for a booking ID")
data class BookingCount(
  @Schema(description = "The booking ID", example = "12345")
  val bookingId: Long,
  @Schema(description = "The count for the booking ID", example = "2")
  val count: Long,
)
