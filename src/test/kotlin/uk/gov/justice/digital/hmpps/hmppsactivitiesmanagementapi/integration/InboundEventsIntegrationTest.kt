package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceAllocationSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ReleaseInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.appointmentsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReleasedEvent

/**
 * This integration test is bypassing the step whereby this would be instigated by incoming prisoner events.
 */
class InboundEventsIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var outboundEventsService: OutboundEventsService

  @Autowired
  private lateinit var repository: AllocationRepository

  @Autowired
  private lateinit var eventReviewRepository: EventReviewRepository

  @Autowired
  private lateinit var appointmentOccurrenceRepository: AppointmentOccurrenceRepository

  @Autowired
  private lateinit var appointmentOccurrenceAllocationSearchRepository: AppointmentOccurrenceAllocationSearchRepository

  @Autowired
  private lateinit var service: InboundEventsService

  @BeforeEach
  fun initMocks() {
    reset(outboundEventsService)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `release with unknown reason does not affect allocations but is treated as an interesting event`() {
    assertThat(eventReviewRepository.count()).isEqualTo(0)
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber = "A11111A", fullInfo = false)

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    // This event falls back to being processed as an interesting event due to the unknown reason for release
    service.process(
      OffenderReleasedEvent(
        ReleaseInformation(
          nomsNumber = "A11111A",
          reason = "UNKNOWN",
          prisonId = pentonvillePrisonCode,
        ),
      ),
    )

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    assertThat(eventReviewRepository.count()).isEqualTo(1L)
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `permanent release of prisoner from remand`() {
    prisonApiMockServer.stubGetPrisonerDetails(
      prisonerNumber = "A11111A",
      fullInfo = true,
      extraInfo = true,
      jsonFileSuffix = "-released-from-remand",
    )

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    service.process(offenderReleasedEvent(prisonerNumber = "A11111A"))

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ENDED))
      assertThat(it.deallocatedReason).isEqualTo(DeallocationReason.RELEASED)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql", "classpath:test_data/seed-appointment-search.sql")
  fun `permanent release of prisoner from custodial sentence`() {
    prisonApiMockServer.stubGetPrisonerDetails(
      prisonerNumber = "A11111A",
      fullInfo = true,
      extraInfo = true,
      jsonFileSuffix = "-released-from-custodial-sentence",
    )

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    service.process(offenderReleasedEvent(prisonerNumber = "A11111A"))

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ENDED))
      assertThat(it.deallocatedReason).isEqualTo(DeallocationReason.RELEASED)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `permanent release of prisoner due to death in prison`() {
    prisonApiMockServer.stubGetPrisonerDetails(
      prisonerNumber = "A11111A",
      fullInfo = true,
      extraInfo = true,
      jsonFileSuffix = "-released-on-death-in-prison",
    )

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    service.process(offenderReleasedEvent(prisonerNumber = "A11111A"))

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ENDED))
      assertThat(it.deallocatedReason).isEqualTo(DeallocationReason.DIED)
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)
  }

  @Test
  @Sql("classpath:test_data/seed-appointments-changed-event.sql")
  fun `appointments cancelled when appointments changed event received with action set to YES`() {
    val appointmentOccurrenceIds = listOf(200L, 201L, 202L, 203L, 210L, 211L, 212L)
    prisonApiMockServer.stubGetPrisonerDetails(
      prisonerNumber = "A1234BC",
      fullInfo = true,
      extraInfo = true,
      jsonFileSuffix = "",
    )

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A1234BC").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    var allocationsMap = appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(
      appointmentOccurrenceIds,
    ).groupBy { it.appointmentOccurrenceSearch.appointmentOccurrenceId }

    assertThat(allocationsMap[200]).hasSize(1)
    assertThat(allocationsMap[201]).hasSize(1)
    assertThat(allocationsMap[202]).hasSize(1)
    assertThat(allocationsMap[203]).hasSize(1)
    assertThat(allocationsMap[210]).hasSize(2)
    assertThat(allocationsMap[211]).hasSize(3)
    assertThat(allocationsMap[212]).hasSize(2)

    service.process(appointmentsChangedEvent(prisonerNumber = "A1234BC"))

    allocationsMap = appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(
      appointmentOccurrenceIds,
    ).groupBy { it.appointmentOccurrenceSearch.appointmentOccurrenceId }

    assertThat(allocationsMap[200]).isNull()
    assertThat(allocationsMap[201]).hasSize(1)
    assertThat(allocationsMap[202]).isNull()
    assertThat(allocationsMap[203]).hasSize(1)
    assertThat(allocationsMap[210]).hasSize(2)
    assertThat(allocationsMap[211]).hasSize(2)
    assertThat(allocationsMap[212]).hasSize(1)

    assertThat(appointmentOccurrenceRepository.existsById(200)).isFalse()
    assertThat(appointmentOccurrenceRepository.existsById(202)).isFalse()

    assertThat(appointmentOccurrenceRepository.existsById(201)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(203)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(210)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(211)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(212)).isTrue()

    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, 300L)
    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, 302L)
    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, 322L)
    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, 324L)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  @Sql("classpath:test_data/seed-appointments-changed-event.sql")
  fun `appointments cancelled when offender released event received`() {
    val appointmentOccurrenceIds = listOf(200L, 201L, 202L, 203L, 210L, 211L, 212L)
    prisonApiMockServer.stubGetPrisonerDetails(
      prisonerNumber = "A1234BC",
      fullInfo = true,
      extraInfo = true,
      jsonFileSuffix = "",
    )

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A1234BC").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    var allocationsMap = appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(
      appointmentOccurrenceIds,
    ).groupBy { it.appointmentOccurrenceSearch.appointmentOccurrenceId }

    assertThat(allocationsMap[200]).hasSize(1)
    assertThat(allocationsMap[201]).hasSize(1)
    assertThat(allocationsMap[202]).hasSize(1)
    assertThat(allocationsMap[203]).hasSize(1)
    assertThat(allocationsMap[210]).hasSize(2)
    assertThat(allocationsMap[211]).hasSize(3)
    assertThat(allocationsMap[212]).hasSize(2)

    service.process(offenderReleasedEvent(prisonerNumber = "A1234BC", prisonCode = "MDI"))

    allocationsMap = appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(
      appointmentOccurrenceIds,
    ).groupBy { it.appointmentOccurrenceSearch.appointmentOccurrenceId }

    assertThat(allocationsMap[200]).isNull()
    assertThat(allocationsMap[201]).hasSize(1)
    assertThat(allocationsMap[202]).isNull()
    assertThat(allocationsMap[203]).hasSize(1)
    assertThat(allocationsMap[210]).hasSize(2)
    assertThat(allocationsMap[211]).hasSize(2)
    assertThat(allocationsMap[212]).hasSize(1)

    assertThat(appointmentOccurrenceRepository.existsById(200)).isFalse()
    assertThat(appointmentOccurrenceRepository.existsById(202)).isFalse()

    assertThat(appointmentOccurrenceRepository.existsById(201)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(203)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(210)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(211)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(212)).isTrue()

    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, 300L)
    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, 302L)
    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, 322L)
    verify(outboundEventsService).send(OutboundEvent.APPOINTMENT_INSTANCE_DELETED, 324L)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  @Sql("classpath:test_data/seed-appointments-changed-event.sql")
  fun `no appointments are cancelled when appointments changed event received with action set to NO`() {
    val appointmentOccurrenceIds = listOf(200L, 201L, 202L, 203L, 210L, 211L, 212L)
    prisonApiMockServer.stubGetPrisonerDetails(
      prisonerNumber = "A1234BC",
      fullInfo = true,
      extraInfo = true,
      jsonFileSuffix = "",
    )

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A1234BC").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    var allocationsMap = appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(
      appointmentOccurrenceIds,
    ).groupBy { it.appointmentOccurrenceSearch.appointmentOccurrenceId }

    assertThat(allocationsMap[200]).hasSize(1)
    assertThat(allocationsMap[201]).hasSize(1)
    assertThat(allocationsMap[202]).hasSize(1)
    assertThat(allocationsMap[203]).hasSize(1)
    assertThat(allocationsMap[210]).hasSize(2)
    assertThat(allocationsMap[211]).hasSize(3)
    assertThat(allocationsMap[212]).hasSize(2)

    service.process(appointmentsChangedEvent(prisonerNumber = "A1234BC", action = "NO"))

    allocationsMap = appointmentOccurrenceAllocationSearchRepository.findByAppointmentOccurrenceIds(
      appointmentOccurrenceIds,
    ).groupBy { it.appointmentOccurrenceSearch.appointmentOccurrenceId }

    assertThat(allocationsMap[200]).hasSize(1)
    assertThat(allocationsMap[201]).hasSize(1)
    assertThat(allocationsMap[202]).hasSize(1)
    assertThat(allocationsMap[203]).hasSize(1)
    assertThat(allocationsMap[210]).hasSize(2)
    assertThat(allocationsMap[211]).hasSize(3)
    assertThat(allocationsMap[212]).hasSize(2)

    assertThat(appointmentOccurrenceRepository.existsById(200)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(201)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(202)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(203)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(210)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(211)).isTrue()
    assertThat(appointmentOccurrenceRepository.existsById(212)).isTrue()

    verifyNoInteractions(outboundEventsService)
  }
}
