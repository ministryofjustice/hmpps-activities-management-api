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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentOccurrence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentRepeatPeriod
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.JobType.CREATE_APPOINTMENT_OCCURRENCES
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.JobRepository
import java.util.Optional

class CreateAppointmentOccurrencesJobTest {
  private val jobRepository: JobRepository = mock()
  private val safeJobRunner = spy(SafeJobRunner(jobRepository))
  private val appointmentRepository: AppointmentRepository = mock()
  private val appointmentOccurrenceRepository: AppointmentOccurrenceRepository = mock()
  private val jobDefinitionCaptor = argumentCaptor<JobDefinition>()
  private val job = CreateAppointmentOccurrencesJob(safeJobRunner, appointmentRepository, appointmentOccurrenceRepository)

  @Captor
  private lateinit var appointmentOccurrenceEntityCaptor: ArgumentCaptor<AppointmentOccurrence>

  private val prisonerNumberToBookingIdMap = (1L..5L).associateBy { "A12${it.toString().padStart(3, '0')}BC" }
  private val prisonerBookings = prisonerNumberToBookingIdMap.map { it.key to it.value.toString() }.toMap()

  @BeforeEach
  fun setUp() {
    MockitoAnnotations.openMocks(this)
  }

  @Test
  fun `job type is create appointment occurrences`() {
    val entity = appointmentEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap)
    whenever(appointmentRepository.findById(entity.appointmentId)).thenReturn(Optional.of(entity))

    job.execute(entity.appointmentId, prisonerBookings)

    verify(safeJobRunner).runJob(jobDefinitionCaptor.capture())

    assertThat(jobDefinitionCaptor.firstValue.jobType).isEqualTo(CREATE_APPOINTMENT_OCCURRENCES)
  }

  @Test
  fun `job does not create any occurrences when all exist`() {
    val entity = appointmentEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, repeatPeriod = AppointmentRepeatPeriod.DAILY, numberOfOccurrences = 3)
    whenever(appointmentRepository.findById(entity.appointmentId)).thenReturn(Optional.of(entity))
    entity.occurrences().forEach {
      whenever(appointmentOccurrenceRepository.findByAppointmentAndSequenceNumber(entity, it.sequenceNumber)).thenReturn(it)
    }

    job.execute(entity.appointmentId, prisonerBookings)

    verify(appointmentOccurrenceRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `job creates all remaining occurrences`() {
    val entity = appointmentEntity(prisonerNumberToBookingIdMap = prisonerNumberToBookingIdMap, repeatPeriod = AppointmentRepeatPeriod.DAILY, numberOfOccurrences = 3)
    entity.occurrences().filter { it.sequenceNumber > 1 }.forEach {
      entity.removeOccurrence(it)
    }
    entity.occurrences().forEach {
      whenever(appointmentOccurrenceRepository.findByAppointmentAndSequenceNumber(entity, it.sequenceNumber)).thenReturn(it)
    }
    whenever(appointmentRepository.findById(entity.appointmentId)).thenReturn(Optional.of(entity))
    whenever(appointmentOccurrenceRepository.saveAndFlush(appointmentOccurrenceEntityCaptor.capture())).thenReturn(mock())

    job.execute(entity.appointmentId, prisonerBookings)

    appointmentOccurrenceEntityCaptor.allValues hasSize 2
    assertThat(appointmentOccurrenceEntityCaptor.allValues.map { it.sequenceNumber }).contains(2, 3)
  }
}
