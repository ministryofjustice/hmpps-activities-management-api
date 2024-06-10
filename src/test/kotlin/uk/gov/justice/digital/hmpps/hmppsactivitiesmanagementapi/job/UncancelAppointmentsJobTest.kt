package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentCancelDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUncancelRequest
import java.time.LocalDateTime

class UncancelAppointmentsJobTest : JobsTestBase() {
  private val service: AppointmentCancelDomainService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = UncancelAppointmentsJob(safeJobRunner, service)

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointmentSeries = appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 4, cancelledBy = "CANCEL.USER", cancelledTime = LocalDateTime.now())
  private val appointment = appointmentSeries.appointments()[1]
  private val applyToThisAndAllFuture = appointmentSeries.applyToAppointments(appointment, ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS, "", true).toSet()

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
    mockJobs(JobType.UNCANCEL_APPOINTMENTS)
  }

  @Test
  fun `job type is uncancel appointments`() {
    job.execute(
      appointmentSeries.appointmentSeriesId,
      appointment.appointmentId,
      applyToThisAndAllFuture.filterNot { it.appointmentId == appointment.appointmentId }.map { it.appointmentId }.toSet(),
      AppointmentUncancelRequest(),
      LocalDateTime.now(),
      "TEST.USER",
      3,
      9,
      System.currentTimeMillis(),
    )

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.UNCANCEL_APPOINTMENTS)
  }

  @Test
  fun `job calls uncancel appointment ids`() {
    val appointmentIdsToCancel =
      applyToThisAndAllFuture.filterNot { it.appointmentId == appointment.appointmentId }
        .map { it.appointmentId }.toSet()
    val request = AppointmentUncancelRequest()
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

    verify(service).uncancelAppointmentIds(
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
