package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import com.microsoft.applicationinsights.TelemetryClient
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentOccurrenceEditedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

@ExtendWith(FakeSecurityContext::class)
class AppointmentOccurrenceUpdateDomainServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val auditService: AuditService = mock()

  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()
  private val service = spy(AppointmentOccurrenceUpdateDomainService(appointmentRepository, telemetryClient, auditService))

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointment = appointmentEntity(updatedBy = null, prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, repeatPeriod = AppointmentRepeatPeriod.DAILY, numberOfOccurrences = 4)
  private val appointmentOccurrence = appointment.occurrences()[1]
  private val applyToThis = appointment.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_OCCURRENCE, "").toSet()
  private val applyToThisAndAllFuture = appointment.applyToOccurrences(appointmentOccurrence, ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES, "").toSet()
  private val applyToAllFuture = appointment.applyToOccurrences(appointmentOccurrence, ApplyTo.ALL_FUTURE_OCCURRENCES, "").toSet()
  private val updatedBy = "TEST.USER"

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
        updatedBy,
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
        updatedBy,
        3,
        10,
        startTimeInMs,
        trackEvent = true,
        auditEvent = false,
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
        updatedBy,
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
  @DisplayName("update occurrences - used by service and async update appointment occurrences job")
  inner class UpdateAppointmentOccurrences {
    @Test
    fun `updates category code`() {
      val ids = applyToThisAndAllFuture.map { it.appointmentOccurrenceId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest(categoryCode = "NEW")
      val response = service.updateAppointmentOccurrences(
        appointment,
        appointmentOccurrence.appointmentOccurrenceId,
        applyToThisAndAllFuture,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        3,
        10,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointment.occurrences().filter { ids.contains(it.appointmentOccurrenceId) }.map { it.categoryCode }.distinct().single() isEqualTo "NEW"
      appointment.occurrences().filterNot { ids.contains(it.appointmentOccurrenceId) }.map { it.categoryCode }.distinct().single() isEqualTo "TEST"

      response.occurrences.filter { ids.contains(it.id) }.map { it.categoryCode }.distinct().single() isEqualTo "NEW"
      response.occurrences.filterNot { ids.contains(it.id) }.map { it.categoryCode }.distinct().single() isEqualTo "TEST"
    }

    @Test
    fun `sets updated and updated by on appointment and occurrence when property changed`() {
      val ids = applyToThisAndAllFuture.map { it.appointmentOccurrenceId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      val updated = LocalDateTime.now()
      val response = service.updateAppointmentOccurrences(
        appointment,
        appointmentOccurrence.appointmentOccurrenceId,
        applyToThisAndAllFuture,
        request,
        emptyMap(),
        updated,
        updatedBy,
        3,
        10,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointment.updated isEqualTo updated
      appointment.updatedBy isEqualTo updatedBy
      with(appointment.occurrences().filter { ids.contains(it.appointmentOccurrenceId) }) {
        this.map { it.updated }.distinct().single() isEqualTo updated
        this.map { it.updatedBy }.distinct().single() isEqualTo updatedBy
      }
      with(appointment.occurrences().filterNot { ids.contains(it.appointmentOccurrenceId) }) {
        this.map { it.updated }.distinct().single() isEqualTo null
        this.map { it.updatedBy }.distinct().single() isEqualTo null
      }

      with(response.occurrences.filter { ids.contains(it.id) }) {
        this.map { it.updated }.distinct().single() isEqualTo updated
        this.map { it.updatedBy }.distinct().single() isEqualTo updatedBy
      }
      with(response.occurrences.filterNot { ids.contains(it.id) }) {
        this.map { it.updated }.distinct().single() isEqualTo null
        this.map { it.updatedBy }.distinct().single() isEqualTo null
      }
    }

    @Test
    fun `does not set updated and updated by on appointment and occurrence when no properties have changed`() {
      val ids = applyToAllFuture.map { it.appointmentOccurrenceId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest()
      val updated = LocalDateTime.now()
      val response = service.updateAppointmentOccurrences(
        appointment,
        appointmentOccurrence.appointmentOccurrenceId,
        applyToAllFuture,
        request,
        emptyMap(),
        updated,
        updatedBy,
        3,
        10,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointment.updated isEqualTo null
      appointment.updatedBy isEqualTo null
      appointment.occurrences().map { it.updated }.distinct().single() isEqualTo null
      appointment.occurrences().map { it.updatedBy }.distinct().single() isEqualTo null

      response.occurrences.map { it.updated }.distinct().single() isEqualTo null
      response.occurrences.map { it.updatedBy }.distinct().single() isEqualTo null
    }

    @Test
    fun `track custom event using supplied counts and start time`() {
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      val startTimeInMs = System.currentTimeMillis()
      service.updateAppointmentOccurrences(
        appointment,
        appointmentOccurrence.appointmentOccurrenceId,
        applyToThis,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        1,
        3,
        startTimeInMs,
        trackEvent = true,
        auditEvent = false,
      )

      verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENT_EDITED.value), telemetryPropertyMap.capture(), telemetryMetricsMap.capture())

      with(telemetryMetricsMap.firstValue) {
        this[APPOINTMENT_COUNT_METRIC_KEY] isEqualTo 1.0
        this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY] isEqualTo 3.0
        assertThat(this[EVENT_TIME_MS_METRIC_KEY]).isCloseTo((System.currentTimeMillis() - startTimeInMs).toDouble(), within(1000.0))
      }
    }

    @Test
    fun `do not track custom event`() {
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      val startTimeInMs = System.currentTimeMillis()
      service.updateAppointmentOccurrences(
        appointment,
        appointmentOccurrence.appointmentOccurrenceId,
        applyToThis,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        1,
        3,
        startTimeInMs,
        trackEvent = false,
        auditEvent = false,
      )

      verifyNoInteractions(telemetryClient)
    }

    @Test
    fun `track audit event`() {
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      service.updateAppointmentOccurrences(
        appointment,
        appointmentOccurrence.appointmentOccurrenceId,
        applyToThis,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        1,
        3,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = true,
      )

      verify(auditService).logEvent(any<AppointmentOccurrenceEditedEvent>())
    }

    @Test
    fun `do not track audit event`() {
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      service.updateAppointmentOccurrences(
        appointment,
        appointmentOccurrence.appointmentOccurrenceId,
        applyToThis,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        1,
        3,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      verifyNoInteractions(auditService)
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
      service.getUpdateInstancesCount(
        request,
        appointment,
        applyToThis,
      ) isEqualTo applyToThis.flatMap { it.allocations() }.size
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
