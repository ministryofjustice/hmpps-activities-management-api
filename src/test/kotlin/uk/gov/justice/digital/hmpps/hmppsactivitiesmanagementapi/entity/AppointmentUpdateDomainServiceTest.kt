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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentEditedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentOccurrenceUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.EVENT_TIME_MS_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

@ExtendWith(FakeSecurityContext::class)
class AppointmentUpdateDomainServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val telemetryClient: TelemetryClient = mock()
  private val auditService: AuditService = mock()

  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()
  private val service = spy(AppointmentUpdateDomainService(appointmentRepository, telemetryClient, auditService))

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointmentSeries = appointmentSeriesEntity(updatedBy = null, prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 4)
  private val appointment = appointmentSeries.appointments()[1]
  private val applyToThis = appointmentSeries.applyToAppointments(appointment, ApplyTo.THIS_OCCURRENCE, "").toSet()
  private val applyToThisAndAllFuture = appointmentSeries.applyToAppointments(appointment, ApplyTo.THIS_AND_ALL_FUTURE_OCCURRENCES, "").toSet()
  private val applyToAllFuture = appointmentSeries.applyToAppointments(appointment, ApplyTo.ALL_FUTURE_OCCURRENCES, "").toSet()
  private val updatedBy = "TEST.USER"

  @BeforeEach
  fun setUp() {
    whenever(appointmentRepository.findById(appointmentSeries.appointmentSeriesId)).thenReturn(Optional.of(appointmentSeries))
    whenever(appointmentRepository.saveAndFlush(any())).thenAnswer(AdditionalAnswers.returnsFirstArg<AppointmentSeries>())
  }

  @Nested
  @DisplayName("update by ids - used by async update appointments job")
  inner class UpdateAppointmentIds {
    @Test
    fun `updates appointments with supplied ids`() {
      val ids = applyToThisAndAllFuture.map { it.appointmentId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      val updated = LocalDateTime.now()
      val startTimeInMs = System.currentTimeMillis()
      val response = service.updateAppointmentIds(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
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

      verify(service).updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
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
      val ids = applyToThisAndAllFuture.map { it.appointmentId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      val startTimeInMs = System.currentTimeMillis()
      service.updateAppointmentIds(
        appointmentSeries.appointmentSeriesId,
        appointment.appointmentId,
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
  @DisplayName("update appointments - used by service and async update appointments job")
  inner class UpdateAppointments {
    @Test
    fun `updates category code`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest(categoryCode = "NEW")
      val response = service.updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
        appointmentsToUpdate,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.categoryCode isEqualTo "TEST"
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.map { it.categoryCode }.distinct().single() isEqualTo "NEW"
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.map { it.categoryCode }.distinct().single() isEqualTo "TEST"

      response.categoryCode isEqualTo "TEST"
      response.occurrences.filter { ids.contains(it.id) }.map { it.categoryCode }.distinct().single() isEqualTo "NEW"
      response.occurrences.filterNot { ids.contains(it.id) }.map { it.categoryCode }.distinct().single() isEqualTo "TEST"
    }

    @Test
    fun `updates internal location id`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      val response = service.updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
        appointmentsToUpdate,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.internalLocationId isEqualTo 123
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.map { it.internalLocationId }.distinct().single() isEqualTo 456
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.map { it.internalLocationId }.distinct().single() isEqualTo 123

      response.internalLocationId isEqualTo 123
      response.occurrences.filter { ids.contains(it.id) }.map { it.internalLocationId }.distinct().single() isEqualTo 456
      response.occurrences.filterNot { ids.contains(it.id) }.map { it.internalLocationId }.distinct().single() isEqualTo 123
    }

    @Test
    fun `updates in cell = true`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest(inCell = true)
      val response = service.updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
        appointmentsToUpdate,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.internalLocationId isEqualTo 123
      appointmentSeries.inCell isEqualTo false
      with(appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }) {
        this.map { it.internalLocationId }.distinct().single() isEqualTo null
        this.map { it.inCell }.distinct().single() isEqualTo true
      }
      with(appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }) {
        this.map { it.internalLocationId }.distinct().single() isEqualTo 123
        this.map { it.inCell }.distinct().single() isEqualTo false
      }

      response.internalLocationId isEqualTo 123
      response.inCell isEqualTo false
      with(response.occurrences.filter { ids.contains(it.id) }) {
        this.map { it.internalLocationId }.distinct().single() isEqualTo null
        this.map { it.inCell }.distinct().single() isEqualTo true
      }
      with(response.occurrences.filterNot { ids.contains(it.id) }) {
        this.map { it.internalLocationId }.distinct().single() isEqualTo 123
        this.map { it.inCell }.distinct().single() isEqualTo false
      }
    }

    @Test
    fun `updates start date`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val weekFromNow = LocalDate.now().plusWeeks(2)
      val request = AppointmentOccurrenceUpdateRequest(startDate = weekFromNow)
      val response = service.updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
        appointmentsToUpdate,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.startDate isEqualTo LocalDate.now().plusDays(1)
      with(appointmentSeries.appointments()) {
        get(0).startDate isEqualTo LocalDate.now().plusDays(1)
        get(1).startDate isEqualTo weekFromNow
        get(2).startDate isEqualTo weekFromNow.plusDays(1)
        get(3).startDate isEqualTo weekFromNow.plusDays(2)
      }

      response.startDate isEqualTo LocalDate.now().plusDays(1)
      with(response.occurrences) {
        get(0).startDate isEqualTo LocalDate.now().plusDays(1)
        get(1).startDate isEqualTo weekFromNow
        get(2).startDate isEqualTo weekFromNow.plusDays(1)
        get(3).startDate isEqualTo weekFromNow.plusDays(2)
      }
    }

    @Test
    fun `updates start time`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest(startTime = LocalTime.of(13, 30))
      val response = service.updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
        appointmentsToUpdate,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.startTime isEqualTo LocalTime.of(9, 0)
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.map { it.startTime }.distinct().single() isEqualTo LocalTime.of(13, 30)
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.map { it.startTime }.distinct().single() isEqualTo LocalTime.of(9, 0)

      response.startTime isEqualTo LocalTime.of(9, 0)
      response.occurrences.filter { ids.contains(it.id) }.map { it.startTime }.distinct().single() isEqualTo LocalTime.of(13, 30)
      response.occurrences.filterNot { ids.contains(it.id) }.map { it.startTime }.distinct().single() isEqualTo LocalTime.of(9, 0)
    }

    @Test
    fun `updates end time`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest(endTime = LocalTime.of(15, 0))
      val response = service.updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
        appointmentsToUpdate,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.endTime isEqualTo LocalTime.of(10, 30)
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.map { it.endTime }.distinct().single() isEqualTo LocalTime.of(15, 0)
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.map { it.endTime }.distinct().single() isEqualTo LocalTime.of(10, 30)

      response.endTime isEqualTo LocalTime.of(10, 30)
      response.occurrences.filter { ids.contains(it.id) }.map { it.endTime }.distinct().single() isEqualTo LocalTime.of(15, 0)
      response.occurrences.filterNot { ids.contains(it.id) }.map { it.endTime }.distinct().single() isEqualTo LocalTime.of(10, 30)
    }

    @Test
    fun `updates comment`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest(comment = "Updated appointment level comment")
      val response = service.updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
        appointmentsToUpdate,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.extraInformation isEqualTo "Appointment series level comment"
      appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }.map { it.extraInformation }.distinct().single() isEqualTo "Updated appointment level comment"
      appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }.map { it.extraInformation }.distinct().single() isEqualTo "Appointment level comment"

      response.comment isEqualTo "Appointment series level comment"
      response.occurrences.filter { ids.contains(it.id) }.map { it.comment }.distinct().single() isEqualTo "Updated appointment level comment"
      response.occurrences.filterNot { ids.contains(it.id) }.map { it.comment }.distinct().single() isEqualTo "Appointment level comment"
    }

    @Test
    fun `sets updated and updated by on appointment series and appointment when property changed`() {
      val appointmentsToUpdate = applyToThisAndAllFuture
      val ids = appointmentsToUpdate.map { it.appointmentId }.toSet()
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      val updated = LocalDateTime.now()
      val response = service.updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
        appointmentsToUpdate,
        request,
        emptyMap(),
        updated,
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.updatedTime isEqualTo updated
      appointmentSeries.updatedBy isEqualTo updatedBy
      with(appointmentSeries.appointments().filter { ids.contains(it.appointmentId) }) {
        this.map { it.updatedTime }.distinct().single() isEqualTo updated
        this.map { it.updatedBy }.distinct().single() isEqualTo updatedBy
      }
      with(appointmentSeries.appointments().filterNot { ids.contains(it.appointmentId) }) {
        this.map { it.updatedTime }.distinct().single() isEqualTo null
        this.map { it.updatedBy }.distinct().single() isEqualTo null
      }

      response.updated isEqualTo updated
      response.updatedBy isEqualTo updatedBy
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
    fun `does not set updated and updated by on appointment series and appointment when no properties have changed`() {
      val appointmentsToUpdate = applyToAllFuture
      val request = AppointmentOccurrenceUpdateRequest()
      val updated = LocalDateTime.now()
      val response = service.updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
        appointmentsToUpdate,
        request,
        emptyMap(),
        updated,
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      appointmentSeries.updatedTime isEqualTo null
      appointmentSeries.updatedBy isEqualTo null
      appointmentSeries.appointments().map { it.updatedTime }.distinct().single() isEqualTo null
      appointmentSeries.appointments().map { it.updatedBy }.distinct().single() isEqualTo null

      response.updated isEqualTo null
      response.updatedBy isEqualTo null
      response.occurrences.map { it.updated }.distinct().single() isEqualTo null
      response.occurrences.map { it.updatedBy }.distinct().single() isEqualTo null
    }

    @Test
    fun `track custom event using supplied counts and start time`() {
      val appointmentsToUpdate = applyToThis
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      val startTimeInMs = System.currentTimeMillis()
      service.updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
        appointmentsToUpdate,
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
      val appointmentsToUpdate = applyToThis
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      val startTimeInMs = System.currentTimeMillis()
      service.updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
        appointmentsToUpdate,
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
      val appointmentsToUpdate = applyToThis
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      service.updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
        appointmentsToUpdate,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = true,
      )

      verify(auditService).logEvent(any<AppointmentEditedEvent>())
    }

    @Test
    fun `do not track audit event`() {
      val appointmentsToUpdate = applyToThis
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      service.updateAppointments(
        appointmentSeries,
        appointment.appointmentId,
        appointmentsToUpdate,
        request,
        emptyMap(),
        LocalDateTime.now(),
        updatedBy,
        appointmentsToUpdate.size,
        appointmentsToUpdate.flatMap { it.attendees() }.size,
        System.currentTimeMillis(),
        trackEvent = false,
        auditEvent = false,
      )

      verifyNoInteractions(auditService)
    }
  }

  @Nested
  @DisplayName("instance count")
  inner class UpdateInstanceCount {
    @Test
    fun `no updates`() {
      val request = AppointmentOccurrenceUpdateRequest()
      val appointmentSeries = appointmentSeriesEntity()
      service.getUpdateInstancesCount(request, appointmentSeries, appointmentSeries.appointments()) isEqualTo 0
    }

    @Test
    fun `update category code`() {
      val request = AppointmentOccurrenceUpdateRequest(categoryCode = "NEW")
      service.getUpdateInstancesCount(
        request,
        appointmentSeries,
        applyToThis,
      ) isEqualTo applyToThis.flatMap { it.attendees() }.size
    }

    @Test
    fun `update location`() {
      val request = AppointmentOccurrenceUpdateRequest(internalLocationId = 456)
      service.getUpdateInstancesCount(
        request,
        appointmentSeries,
        applyToThisAndAllFuture,
      ) isEqualTo applyToThisAndAllFuture.flatMap { it.attendees() }.size
    }

    @Test
    fun `remove prisoners`() {
      // Only A1234BC is currently allocated
      val request = AppointmentOccurrenceUpdateRequest(removePrisonerNumbers = listOf("A1234BC", "D4567EF"))
      service.getUpdateInstancesCount(request, appointmentSeries, applyToAllFuture) isEqualTo applyToAllFuture.size
    }

    @Test
    fun `add prisoners`() {
      // C3456DE is already allocated
      val request = AppointmentOccurrenceUpdateRequest(addPrisonerNumbers = listOf("C3456DE", "D4567EF", "E5678FG"))
      service.getUpdateInstancesCount(request, appointmentSeries, applyToThis) isEqualTo applyToThis.size * 2
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
        appointmentSeries,
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
        appointmentSeries,
        applyToThisAndAllFuture,
      ) isEqualTo applyToThisAndAllFuture.flatMap { it.attendees() }.size
    }

    @Test
    fun `includes added prisoners when a property is also updated`() {
      val request = AppointmentOccurrenceUpdateRequest(
        endTime = LocalTime.of(11, 0),
        addPrisonerNumbers = listOf("D4567EF", "E5678FG"),
      )
      service.getUpdateInstancesCount(
        request,
        appointmentSeries,
        applyToAllFuture,
      ) isEqualTo applyToAllFuture.flatMap { it.attendees() }.size + (applyToAllFuture.size * 2)
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
        appointmentSeries,
        applyToAllFuture,
      ) isEqualTo applyToAllFuture.flatMap { it.attendees() }.size + (applyToAllFuture.size * 2)
    }
  }
}
