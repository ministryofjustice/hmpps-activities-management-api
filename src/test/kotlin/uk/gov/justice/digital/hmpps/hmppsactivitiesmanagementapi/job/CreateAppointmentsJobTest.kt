package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.job

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Appointment
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.CREATE_APPOINTMENTS
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSeriesEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentSeriesRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.TransactionHandler
import java.util.Optional

class CreateAppointmentsJobTest {
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository, TransactionHandler()))
  private val appointmentSeriesRepository: AppointmentSeriesRepository = mock()
  private val appointmentRepository: AppointmentRepository = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = CreateAppointmentsJob(safeJobRunner, appointmentSeriesRepository, appointmentRepository)

  @Captor
  private lateinit var appointmentEntityCaptor: ArgumentCaptor<Appointment>

  private val prisonerNumberToBookingIdMap = (1L..5L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
  private val prisonerBookings = prisonerNumberToBookingIdMap.map { it.key to it.value.toString() }.toMap()

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `job type is create appointments`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap)
    whenever(appointmentSeriesRepository.findById(entity.appointmentSeriesId)).thenReturn(Optional.of(entity))

    job.execute(entity.appointmentSeriesId, prisonerBookings)

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(CREATE_APPOINTMENTS)
  }

  @Test
  fun `job does not create any appointments when all exist`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 3)
    whenever(appointmentSeriesRepository.findById(entity.appointmentSeriesId)).thenReturn(Optional.of(entity))
    entity.appointments().forEach {
      whenever(appointmentRepository.findByAppointmentSeriesAndSequenceNumber(entity, it.sequenceNumber)).thenReturn(it)
    }

    job.execute(entity.appointmentSeriesId, prisonerBookings)

    verify(appointmentRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `job creates all remaining appointments`() {
    val entity = appointmentSeriesEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, frequency = AppointmentFrequency.DAILY, numberOfAppointments = 3)
    entity.appointments().filter { it.sequenceNumber > 1 }.forEach {
      entity.removeAppointment(it)
    }
    entity.appointments().forEach {
      whenever(appointmentRepository.findByAppointmentSeriesAndSequenceNumber(entity, it.sequenceNumber)).thenReturn(it)
    }
    whenever(appointmentSeriesRepository.findById(entity.appointmentSeriesId)).thenReturn(Optional.of(entity))
    whenever(appointmentRepository.saveAndFlush(appointmentEntityCaptor.capture())).thenReturn(mock())

    job.execute(entity.appointmentSeriesId, prisonerBookings)

    appointmentEntityCaptor.allValues hasSize 2
    assertThat(appointmentEntityCaptor.allValues.map { it.sequenceNumber }).contains(2, 3)
  }
}
