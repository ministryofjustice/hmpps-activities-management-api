package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Retryable
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentUpdateDomainService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ApplyTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AppointmentUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.MonitoringService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDateTime

class UpdateAppointmentsJobTest {
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository, mock<MonitoringService>(), mock<Retryable>()))
  private val service: AppointmentUpdateDomainService = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = UpdateAppointmentsJob(safeJobRunner, service)

  private val prisonerNumberToBookingIdMap = mapOf("A1234BC" to 1L, "B2345CD" to 2L, "C3456DE" to 3L)
  private val appointmentSeries = appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 4)
  private val appointment = appointmentSeries.appointments()[1]
  private val applyToThisAndAllFuture = appointmentSeries.applyToAppointments(appointment, ApplyTo.THIS_AND_ALL_FUTURE_APPOINTMENTS, "").toSet()

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `job type is update appointments`() {
    job.execute(
      appointmentSeries.appointmentSeriesId,
      appointment.appointmentId,
      applyToThisAndAllFuture.filterNot { it.appointmentId == appointment.appointmentId }.map { it.appointmentId }.toSet(),
      AppointmentUpdateRequest(internalLocationId = 456),
      emptyMap(),
      LocalDateTime.now(),
      "TEST.USER",
      3,
      9,
      System.currentTimeMillis(),
    )

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(JobType.UPDATE_APPOINTMENTS)
  }

  @Test
  fun `job calls update appointment ids`() {
    val appointmentIdsToUpdate =
      applyToThisAndAllFuture.filterNot { it.appointmentId == appointment.appointmentId }
        .map { it.appointmentId }.toSet()
    val request = AppointmentUpdateRequest(internalLocationId = 456, addPrisonerNumbers = listOf("D4567EF", "E5679FG"))
    val prisonerMap = mapOf(
      "D4567EF" to PrisonerSearchPrisonerFixture.instance(prisonerNumber = "D4567EF", bookingId = 459, prisonId = "TPR"),
      "E5679FG" to PrisonerSearchPrisonerFixture.instance(prisonerNumber = "E5679FG", bookingId = 460, prisonId = "TPR"),
    )
    val updated = LocalDateTime.now()
    val startTimeInMs = System.currentTimeMillis()

    job.execute(
      appointmentSeries.appointmentSeriesId,
      appointment.appointmentId,
      appointmentIdsToUpdate,
      request,
      prisonerMap,
      updated,
      "TEST.USER",
      3,
      9,
      startTimeInMs,
    )

    verify(service).updateAppointments(
      appointmentSeries.appointmentSeriesId,
      appointment.appointmentId,
      appointmentIdsToUpdate,
      request,
      prisonerMap,
      updated,
      "TEST.USER",
      3,
      9,
      startTimeInMs,
      true,
      false,
    )
  }
}
