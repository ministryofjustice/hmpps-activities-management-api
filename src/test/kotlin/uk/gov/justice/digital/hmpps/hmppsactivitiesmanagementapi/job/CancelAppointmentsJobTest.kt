package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentCancelRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.appointment.AppointmentCancelDomainService
import java.time.LocalDateTime

class CancelAppointmentsJobTest : JobsTestBase() {
  private val service: AppointmentCancelDomainService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = CancelAppointmentsJob(safeJobRunner, service)

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointmentSeries = appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 4)
  private val appointment = appointmentSeries.appointments()[1]
  private val applyToThisAndAllFuture = appointmentSeries.applyToAppointments(appointment, ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS, "", false).toSet()

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    mockJobs(JobType.CANCEL_APPOINTMENTS)
  }

  @Test
  fun `job type is cancel appointments`() {
    job.execute(
      appointmentSeries.appointmentSeriesId,
      appointment.appointmentId,
      applyToThisAndAllFuture.filterNot { it.appointmentId == appointment.appointmentId }.map { it.appointmentId }.toSet(),
      AppointmentCancelRequest(cancellationReasonId = 1),
      LocalDateTime.now(),
      "TEST.USER",
      3,
      9,
      System.currentTimeMillis(),
    )

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.CANCEL_APPOINTMENTS)
  }

  @Test
  fun `job calls cancel appointment ids`() {
    val appointmentIdsToCancel =
      applyToThisAndAllFuture.filterNot { it.appointmentId == appointment.appointmentId }
        .map { it.appointmentId }.toSet()
    val request = AppointmentCancelRequest(cancellationReasonId = 2)
    val cancelled = LocalDateTime.now()
    val startTimeInMs = System.currentTimeMillis()

    job.execute(
      appointmentSeries.appointmentSeriesId,
      appointment.appointmentId,
      appointmentIdsToCancel,
      request,
      cancelled,
      "TEST.USER",
      3,
      9,
      startTimeInMs,
    )

    verify(service).cancelAppointmentIds(
      appointmentSeries.appointmentSeriesId,
      appointment.appointmentId,
      appointmentIdsToCancel,
      request,
      cancelled,
      "TEST.USER",
      3,
      9,
      startTimeInMs,
    )
  }
}
