package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import java.time.LocalTime

class AppointmentOccurrenceUpdateDomainServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  private val service = AppointmentOccurrenceUpdateDomainService(appointmentRepository, telemetryClient)

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointment = appointmentEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, repeatPeriod = AppointmentRepeatPeriod.DAILY, numberOfOccurrences = 4)
  private val appointmentOccurrence = appointment.occurrences()[1]
  private val applyToThis = appointment.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_OCCURRENCE, "")
  private val applyToThisAndAllFuture = appointment.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES, "")
  private val applyToAllFuture = appointment.applyToOccurrences(appointmentOccurrence, ApplyTo.ALL_FUTURE_OCCURRENCES, "")

  @Test
  fun `no updates`() {
    val request = AppointmentOccurrenceUpdateRequest()
    val appointment = appointmentEntity()
    service.getUpdatedInstanceCount(request, appointment, appointment.occurrences()) isEqualTo 0
  }

  @Test
  fun `update category code`() {
    val request = AppointmentOccurrenceUpdateRequest(categoryCode = "NEW")
    service.getUpdatedInstanceCount(request, appointment, applyToThis) isEqualTo 12
  }

  @Test
  fun `update location`() {
    val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
    service.getUpdatedInstanceCount(request, appointment, applyToThisAndAllFuture) isEqualTo applyToThisAndAllFuture.flatMap { it.allocations() }.size
  }

  @Test
  fun `remove prisoners`() {
    // Only A1234BC is currently allocated
    val request = AppointmentOccurrenceUpdateRequest(removePrisonerNumbers = listOf("A1234BC", "D4567EF"))
    service.getUpdatedInstanceCount(request, appointment, applyToAllFuture) isEqualTo applyToAllFuture.size
  }

  @Test
  fun `add prisoners`() {
    // C3456DE is already allocated
    val request = AppointmentOccurrenceUpdateRequest(addPrisonerNumbers = listOf("C3456DE", "D4567EF", "E5678FG"))
    service.getUpdatedInstanceCount(request, appointment, applyToThis) isEqualTo applyToThis.size * 2
  }

  @Test
  fun `instance count does not include removed prisoners when a property is also updated`() {
    val request = AppointmentOccurrenceUpdateRequest(startTime = LocalTime.of(8, 30), removePrisonerNumbers = listOf("A1234BC", "D4567EF"))
    service.getUpdatedInstanceCount(request, appointment, applyToThisAndAllFuture) isEqualTo applyToThisAndAllFuture.flatMap { it.allocations() }.size
  }

  @Test
  fun `instance count includes added prisoners when a property is also updated`() {
    val request = AppointmentOccurrenceUpdateRequest(endTime = LocalTime.of(11, 0), addPrisonerNumbers = listOf("D4567EF", "E5678FG"))
    service.getUpdatedInstanceCount(request, appointment, applyToAllFuture) isEqualTo applyToAllFuture.flatMap { it.allocations() }.size + (applyToAllFuture.size * 2)
  }

  @Test
  fun `update a property, remove a prisoner and add two prisoners`() {
    val request = AppointmentOccurrenceUpdateRequest(
      comment = "New",
      removePrisonerNumbers = listOf("A1234BC", "D4567EF"),
      addPrisonerNumbers = listOf("C3456DE", "D4567EF", "E5678FG"),
    )
    service.getUpdatedInstanceCount(request, appointment, applyToAllFuture) isEqualTo applyToAllFuture.flatMap { it.allocations() }.size + (applyToAllFuture.size * 2)
  }
}
