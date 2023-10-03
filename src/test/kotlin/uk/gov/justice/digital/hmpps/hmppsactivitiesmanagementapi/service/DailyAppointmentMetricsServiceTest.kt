package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SET_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CANCELLED_APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.DELETED_APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.TelemetryEvent
import java.time.LocalDate

class DailyAppointmentMetricsServiceTest {
  private val appointmentRepository: AppointmentRepository = mock()
  private val telemetryClient: TelemetryClient = mock()

  private val telemetryPropertyMap = argumentCaptor<Map<String, String>>()
  private val telemetryMetricsMap = argumentCaptor<Map<String, Double>>()

  private val dailyAppointmentMetricsService = DailyAppointmentMetricsService(appointmentRepository, telemetryClient)

  private val cancelledAppointment: Appointment = mock()
  private val deletedAppointment: Appointment = mock()
  private val appointmentWithSeries: Appointment = mock()
  private val appointmentWithSet: Appointment = mock()
  private val appointmentWithAttendees: Appointment = mock()
  private val appointmentSeriesWithSchedule: AppointmentSeries = mock()
  private val appointmentSeriesWithSet: AppointmentSeries = mock()
  private val appointmentSeriesSchedule: AppointmentSeriesSchedule = mock()
  private val emptyAppointmentSeries: AppointmentSeries = mock()

  @Test
  fun `should generate daily metrics`() {
    whenever(cancelledAppointment.isCancelled()).thenReturn(true)
    whenever(deletedAppointment.isDeleted).thenReturn(true)

    whenever(appointmentWithSeries.isDeleted).thenReturn(false)
    whenever(appointmentWithSeries.isCancelled()).thenReturn(false)
    whenever(appointmentWithSeries.appointmentSeries).thenReturn(appointmentSeriesWithSchedule)
    whenever(appointmentSeriesWithSchedule.schedule).thenReturn(appointmentSeriesSchedule)
    whenever(appointmentSeriesSchedule.numberOfAppointments).thenReturn(12)

    whenever(appointmentWithSet.isDeleted).thenReturn(false)
    whenever(appointmentWithSet.isCancelled()).thenReturn(false)
    whenever(appointmentWithSet.appointmentSeries).thenReturn(appointmentSeriesWithSet)
    whenever(appointmentSeriesWithSet.appointmentSet).thenReturn(mock<AppointmentSet>())

    whenever(appointmentWithAttendees.isDeleted).thenReturn(false)
    whenever(appointmentWithAttendees.isCancelled()).thenReturn(false)
    whenever(appointmentWithAttendees.appointmentSeries).thenReturn(emptyAppointmentSeries)
    whenever(appointmentWithAttendees.attendees()).thenReturn(listOf(mock(), mock(), mock()))

    val appointments = listOf(
      cancelledAppointment,
      deletedAppointment,
      appointmentWithSet,
      appointmentWithSeries,
      appointmentWithAttendees,
    )
    val yesterday = LocalDate.now().minusDays(1)

    whenever(appointmentRepository.findByPrisonCodeAndCategoryCodeAndDate(moorlandPrisonCode, "CHAP", yesterday)).thenReturn(appointments)

    dailyAppointmentMetricsService.generateAppointmentMetrics(moorlandPrisonCode, "CHAP", yesterday)

    verify(telemetryClient).trackEvent(eq(TelemetryEvent.APPOINTMENTS_AGGREGATE_METRICS.value), telemetryPropertyMap.capture(), telemetryMetricsMap.capture())

    with(telemetryMetricsMap.firstValue) {
      this[APPOINTMENT_COUNT_METRIC_KEY] isEqualTo 3.0
      this[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY] isEqualTo 3.0
      this[APPOINTMENT_SERIES_COUNT_METRIC_KEY] isEqualTo 1.0
      this[APPOINTMENT_SET_COUNT_METRIC_KEY] isEqualTo 1.0
      this[CANCELLED_APPOINTMENT_COUNT_METRIC_KEY] isEqualTo 1.0
      this[DELETED_APPOINTMENT_COUNT_METRIC_KEY] isEqualTo 1.0
    }
  }
}
