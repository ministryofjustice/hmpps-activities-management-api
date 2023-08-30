package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class AppointmentOccurrenceUpdateDomainServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  private val service = spy(AppointmentOccurrenceUpdateDomainService(appointmentRepository, telemetryClient))

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointment = appointmentEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, repeatPeriod = AppointmentRepeatPeriod.DAILY, numberOfOccurrences = 4)
  private val appointmentOccurrence = appointment.occurrences()[1]
  private val applyToThis = appointment.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_OCCURRENCE, "").toSet()
  private val applyToThisAndAllFuture = appointment.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES, "").toSet()
  private val applyToAllFuture = appointment.applyToOccurrences(appointmentOccurrence, ApplyTo.ALL_FUTURE_OCCURRENCES, "").toSet()

  @BeforeEach
  fun setUp() {
    whenever(appointmentRepository.findById(appointment.appointmentId)).thenReturn(Optional.of(appointment))
    whenever(appointmentRepository.saveAndFlush(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<Appointment>())
  }

  @Nested
  @DisplayName("update by ids - used by async update appointment occurrences job")
  inner class UpdateAppointmentOccurrenceIds {
    @Test
    fun `updates occurrences with supplied ids`() {
      val ids = applyToThisAndAllFuture.map { it.appointmentOccurrenceId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      val updated = LocalDateTime.now()
      val startTimeInMs = System.currentTimeMillis()
      val response = service.updateAppointmentOccurrenceIds(
        appointment.appointmentId,
        appointmentOccurrence.appointmentOccurrenceId,
        ids,
        request,
        emptyMap(),
        updated,
        "TEST.USER",
        3,
        10,
        startTimeInMs,
      )

      response.occurrences.filter { ids.contains(it.id) }.map { it.internalLocationId }.distinct().single() isEqualTo 456
      response.occurrences.filterNot { ids.contains(it.id) }.map { it.internalLocationId }.distinct().single() isEqualTo 123

      verify(service).updateAppointmentOccurrences(
        appointment,
        appointmentOccurrence.appointmentOccurrenceId,
        applyToThisAndAllFuture.toSet(),
        request,
        emptyMap(),
        updated,
        "TEST.USER",
        3,
        10,
        startTimeInMs,
        true,
      )
    }

    @Test
    fun `track custom event using supplied counts and start time`() {
      val ids = applyToThisAndAllFuture.map { it.appointmentOccurrenceId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      val startTimeInMs = System.currentTimeMillis()
      service.updateAppointmentOccurrenceIds(
        appointment.appointmentId,
        appointmentOccurrence.appointmentOccurrenceId,
        ids,
        request,
        emptyMap(),
        LocalDateTime.now(),
        "TEST.USER",
        3,
        10,
        startTimeInMs,
      )

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_EDITED.value), telemetryPropertyMap.capture(), telemetryMetricsMap.capture())

      with(telemetryMetricsMap.firstValue) {
        this[APPOINTMENT_COUNT_METRIC_KEY] isEqualTo 3.0
        this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY] isEqualTo 10.0
        assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isCloseTo((System.currentTimeMillis() - startTimeInMs).toDouble(), within(1000.0))
      }
    }
  }

  @Nested
  @DisplayName("instance count")
  inner class UpdateAppointmentOccurrenceInstanceCount {
    @Test
    fun `no updates`() {
      val request = AppointmentOccurrenceUpdateRequest()
      val appointment = appointmentEntity()
      service.getUpdateInstancesCount(request, appointment, appointment.occurrences()) isEqualTo 0
    }

    @Test
    fun `update category code`() {
      val request = AppointmentOccurrenceUpdateRequest(categoryCode = "NEW")
      service.getUpdateInstancesCount(request, appointment, applyToThis) isEqualTo 12
    }

    @Test
    fun `update location`() {
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      service.getUpdateInstancesCount(
        request,
        appointment,
        applyToThisAndAllFuture,
      ) isEqualTo applyToThisAndAllFuture.flatMap { it.allocations() }.size
    }

    @Test
    fun `remove prisoners`() {
      // Only A1234BC is currently allocated
      val request = AppointmentOccurrenceUpdateRequest(removePrisonerNumbers = listOf("A1234BC", "D4567EF"))
      service.getUpdateInstancesCount(request, appointment, applyToAllFuture) isEqualTo applyToAllFuture.size
    }

    @Test
    fun `add prisoners`() {
      // C3456DE is already allocated
      val request = AppointmentOccurrenceUpdateRequest(addPrisonerNumbers = listOf("C3456DE", "D4567EF", "E5678FG"))
      service.getUpdateInstancesCount(request, appointment, applyToThis) isEqualTo applyToThis.size * 2
    }

    @Test
    fun `remove and add prisoners`() {
      // Only A1234BC is currently allocated
      val request = AppointmentOccurrenceUpdateRequest(
        removePrisonerNumbers = listOf("A1234BC"),
        addPrisonerNumbers = listOf("D4567EF", "E5678FG"),
      )
      service.getUpdateInstancesCount(
        request,
        appointment,
        applyToThis,
      ) isEqualTo applyToThis.size + applyToThis.size * 2
    }

    @Test
    fun `does not include removed prisoners when a property is also updated`() {
      val request = AppointmentOccurrenceUpdateRequest(
        startTime = LocalTime.of(8, 30),
        removePrisonerNumbers = listOf("A1234BC", "D4567EF"),
      )
      service.getUpdateInstancesCount(
        request,
        appointment,
        applyToThisAndAllFuture,
      ) isEqualTo applyToThisAndAllFuture.flatMap { it.allocations() }.size
    }

    @Test
    fun `includes added prisoners when a property is also updated`() {
      val request = AppointmentOccurrenceUpdateRequest(
        endTime = LocalTime.of(11, 0),
        addPrisonerNumbers = listOf("D4567EF", "E5678FG"),
      )
      service.getUpdateInstancesCount(
        request,
        appointment,
        applyToAllFuture,
      ) isEqualTo applyToAllFuture.flatMap { it.allocations() }.size + (applyToAllFuture.size * 2)
    }

    @Test
    fun `update a property, remove a prisoner and add two prisoners`() {
      val request = AppointmentOccurrenceUpdateRequest(
        comment = "New",
        removePrisonerNumbers = listOf("A1234BC", "D4567EF"),
        addPrisonerNumbers = listOf("C3456DE", "D4567EF", "E5678FG"),
      )
      service.getUpdateInstancesCount(
        request,
        appointment,
        applyToAllFuture,
      ) isEqualTo applyToAllFuture.flatMap { it.allocations() }.size + (applyToAllFuture.size * 2)
    }
  }
}
