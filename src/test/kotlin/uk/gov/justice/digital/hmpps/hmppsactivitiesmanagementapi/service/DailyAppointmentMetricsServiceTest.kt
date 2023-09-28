package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSeriesSchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentSet
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_INSTANCE_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SERIES_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.APPOINTMENT_SET_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.CANCELLED_APPOINTMENT_COUNT_METRIC_KEY
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.telemetry.DELETED_APPOINTMENT_COUNT

class DailyAppointmentMetricsServiceTest {

  private val dailyAppointmentMetricsService = DailyAppointmentMetricsService()

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
    val metricsMap = createMetricsMap()

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
    dailyAppointmentMetricsService.generateAppointmentMetrics(metricsMap, appointments)

    Assertions.assertThat(metricsMap[APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(3.0)
    Assertions.assertThat(metricsMap[APPOINTMENT_INSTANCE_COUNT_METRIC_KEY]).isEqualTo(3.0)
    Assertions.assertThat(metricsMap[APPOINTMENT_SERIES_COUNT_METRIC_KEY]).isEqualTo(1.0)
    Assertions.assertThat(metricsMap[APPOINTMENT_SET_COUNT_METRIC_KEY]).isEqualTo(1.0)
    Assertions.assertThat(metricsMap[CANCELLED_APPOINTMENT_COUNT_METRIC_KEY]).isEqualTo(1.0)
  }

  private fun createMetricsMap() = mutableMapOf(
    APPOINTMENT_COUNT_METRIC_KEY to 0.0,
    APPOINTMENT_INSTANCE_COUNT_METRIC_KEY to 0.0,
    APPOINTMENT_SERIES_COUNT_METRIC_KEY to 0.0,
    APPOINTMENT_SET_COUNT_METRIC_KEY to 0.0,
    CANCELLED_APPOINTMENT_COUNT_METRIC_KEY to 0.0,
    DELETED_APPOINTMENT_COUNT to 0.0,
  )
}
