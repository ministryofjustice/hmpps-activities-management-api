package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentCancellationReasonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository

class AppointmentOccurrenceCancelDomainServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentCancellationReasonRepository: AppointmentCancellationReasonRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  private val service = AppointmentOccurrenceCancelDomainService(appointmentRepository, appointmentCancellationReasonRepository, telemetryClient)

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointment = appointmentEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, repeatPeriod = AppointmentRepeatPeriod.DAILY, numberOfOccurrences = 4)
  private val appointmentOccurrence = appointment.occurrences()[1]
  private val applyToThis = appointment.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_OCCURRENCE, "")
  private val applyToThisAndAllFuture = appointment.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES, "")
  private val applyToAllFuture = appointment.applyToOccurrences(appointmentOccurrence, ApplyTo.ALL_FUTURE_OCCURRENCES, "")

  @Nested
  @DisplayName("instance count")
  inner class CancelAppointmentOccurrenceInstanceCount {
    @Test
    fun `this occurrence`() {
      service.getCancelInstancesCount(applyToThis) isEqualTo applyToThis.flatMap { it.allocations() }.size
    }

    @Test
    fun `this and all future occurrences`() {
      service.getCancelInstancesCount(applyToThisAndAllFuture) isEqualTo applyToThisAndAllFuture.flatMap { it.allocations() }.size
    }

    @Test
    fun `all future occurrences`() {
      service.getCancelInstancesCount(applyToAllFuture) isEqualTo applyToAllFuture.flatMap { it.allocations() }.size
    }
  }
}
