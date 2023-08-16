package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceAllocationSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AppointmentOccurrenceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.Action
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ReleaseInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.activitiesChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.alertsUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.appointmentsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReceivedFromTemporaryAbsence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReleasedEvent
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * This integration test is bypassing the step whereby this would be instigated by incoming prisoner events.
 */
@TestPropertySource(
  properties = [
    "feature.audit.service.hmpps.enabled=true",
    "feature.audit.service.local.enabled=true",
  ],
)
class InboundEventsIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var outboundEventsService: OutboundEventsService

  @Autowired
  private lateinit var allocationRepository: AllocationRepository

  @Autowired
  private lateinit var attendanceRepository: AttendanceRepository

  @Autowired
  private lateinit var eventReviewRepository: EventReviewRepository

  @Autowired
  private lateinit var waitingListRepository: WaitingListRepository

  @Autowired
  private lateinit var appointmentOccurrenceRepository: AppointmentOccurrenceRepository

  @Autowired
  private lateinit var appointmentOccurrenceAllocationSearchRepository: AppointmentOccurrenceAllocationSearchRepository

  @MockBean
  private lateinit var hmppsAuditApiClient: HmppsAuditApiClient

  private val hmppsAuditEventCaptor = argumentCaptor<HmppsAuditEvent>()

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

    assertThatAllocationsAreActiveFor(pentonvillePrisonCode, "A11111A")

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

    assertThatAllocationsAreActiveFor(pentonvillePrisonCode, "A11111A")

    assertThat(eventReviewRepository.count()).isEqualTo(1L)
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  @Sql("classpath:test_data/seed-offender-for-release.sql")
  fun `permanent release of prisoner from remand ends allocations and declines waiting list for offender`() {
    prisonApiMockServer.stubGetPrisonerDetails(
      prisonerNumber = "A11111A",
      fullInfo = true,
      extraInfo = true,
      jsonFileSuffix = "-released-from-remand",
    )

    assertThatWaitingListStatusIs(WaitingListStatus.PENDING, pentonvillePrisonCode, "A11111A")
    assertThatAllocationsAreActiveFor(pentonvillePrisonCode, "A11111A")

    service.process(offenderReleasedEvent(prisonerNumber = "A11111A"))

    assertThatAllocationsAreEndedFor(pentonvillePrisonCode, "A11111A")
    assertThatWaitingListStatusIs(WaitingListStatus.DECLINED, pentonvillePrisonCode, "A11111A")

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)

    verify(hmppsAuditApiClient, times(2)).createEvent(hmppsAuditEventCaptor.capture())

    with(hmppsAuditEventCaptor.firstValue) {
      assertThat(what).isEqualTo("PRISONER_DEALLOCATED")
      assertThat(who).isEqualTo(ServiceName.SERVICE_NAME.value)
      assertThatJson(details).isEqualTo("{\"activityId\":1,\"activityName\":\"Maths\",\"prisonCode\":\"PVI\",\"prisonerNumber\":\"A11111A\",\"scheduleId\":1,\"createdAt\":\"\${json-unit.ignore}\",\"createdBy\":\"Activities Management Service\"}")
    }

    with(hmppsAuditEventCaptor.secondValue) {
      assertThat(what).isEqualTo("PRISONER_DEALLOCATED")
      assertThat(who).isEqualTo(ServiceName.SERVICE_NAME.value)
      assertThatJson(details).isEqualTo("{\"activityId\":1,\"activityName\":\"Maths\",\"prisonCode\":\"PVI\",\"prisonerNumber\":\"A11111A\",\"scheduleId\":2,\"createdAt\":\"\${json-unit.ignore}\",\"createdBy\":\"Activities Management Service\"}")
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `prisoner alerts updated`() {
    prisonApiMockServer.stubGetPrisonerDetails(
      prisonerNumber = "A11111A",
      fullInfo = false,
      extraInfo = null,
      jsonFileSuffix = "",
    )

    service.process(alertsUpdatedEvent(prisonerNumber = "A11111A"))

    val interestingEvent = eventReviewRepository.findAll().last()

    assertThat(interestingEvent.eventType).isEqualTo("prison-offender-search.prisoner.alerts-updated")
    assertThat(interestingEvent.prisonerNumber).isEqualTo("A11111A")
    assertThat(interestingEvent.eventData).isEqualTo("Alerts updated for  HARRISON, TIM (A11111A)")
  }

  @Test
  @Sql("classpath:test_data/seed-offender-for-release.sql", "classpath:test_data/seed-appointment-search.sql")
  fun `permanent release of prisoner from custodial sentence ends allocations and declines waiting list for offender`() {
    prisonApiMockServer.stubGetPrisonerDetails(
      prisonerNumber = "A11111A",
      fullInfo = true,
      extraInfo = true,
      jsonFileSuffix = "-released-from-custodial-sentence",
    )

    assertThatWaitingListStatusIs(WaitingListStatus.PENDING, pentonvillePrisonCode, "A11111A")
    assertThatAllocationsAreActiveFor(pentonvillePrisonCode, "A11111A")

    service.process(offenderReleasedEvent(prisonerNumber = "A11111A"))

    assertThatAllocationsAreEndedFor(pentonvillePrisonCode, "A11111A")
    assertThatWaitingListStatusIs(WaitingListStatus.DECLINED, pentonvillePrisonCode, "A11111A")

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)
  }

  @Test
  @Sql("classpath:test_data/seed-offender-for-release.sql")
  fun `permanent release of prisoner due to death in prison ends allocations and declines waiting list for offender`() {
    prisonApiMockServer.stubGetPrisonerDetails(
      prisonerNumber = "A11111A",
      fullInfo = true,
      extraInfo = true,
      jsonFileSuffix = "-released-on-death-in-prison",
    )

    assertThatWaitingListStatusIs(WaitingListStatus.PENDING, pentonvillePrisonCode, "A11111A")
    assertThatAllocationsAreActiveFor(pentonvillePrisonCode, "A11111A")

    service.process(offenderReleasedEvent(prisonerNumber = "A11111A"))

    assertThatAllocationsAreEndedFor(pentonvillePrisonCode, "A11111A", DeallocationReason.DIED)
    assertThatWaitingListStatusIs(WaitingListStatus.DECLINED, pentonvillePrisonCode, "A11111A")

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)
  }

  @Test
  @Sql("classpath:test_data/seed-offender-released-event-deletes-attendances.sql")
  fun `permanent release of prisoner removes any future attendances at time of event being raised`() {
    prisonApiMockServer.stubGetPrisonerDetails(
      prisonerNumber = "A11111A",
      fullInfo = true,
      extraInfo = true,
      jsonFileSuffix = "-released-from-remand",
    )

    assertThatAllocationsAreActiveFor(pentonvillePrisonCode, "A11111A")

    assertThat(attendanceRepository.findAllById(listOf(1L, 2L, 3L)).map { it.attendanceId }).containsExactlyInAnyOrder(1L, 2L, 3L)

    service.process(offenderReleasedEvent(prisonerNumber = "A11111A"))

    assertThatAllocationsAreEndedFor(pentonvillePrisonCode, "A11111A")

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 2L)

    assertThat(attendanceRepository.findAllById(listOf(1L, 2L, 3L)).map { it.attendanceId }).containsOnly(1L)
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

    allocationRepository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A1234BC").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue
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

    allocationRepository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A1234BC").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue
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

    allocationRepository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A1234BC").onEach {
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

  @Test
  @Sql("classpath:test_data/seed-activity-changed-event.sql")
  fun `two allocations and two future attendance are suspended on receipt of activities changed event for prisoner`() {
    assertThatAllocationsAreActiveFor(pentonvillePrisonCode, "A11111A")

    attendanceRepository.findAllById(listOf(1L, 2L, 3L)).onEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(it.attendanceReason).isNull()
      assertThat(it.issuePayment).isNull()
      assertThat(it.recordedTime).isNull()
      assertThat(it.recordedBy).isNull()
    }

    service.process(activitiesChangedEvent(prisonId = pentonvillePrisonCode, prisonerNumber = "A11111A", action = Action.SUSPEND))

    allocationRepository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.AUTO_SUSPENDED))
      assertThat(it.suspendedReason).isEqualTo("Temporary absence")
      assertThat(it.suspendedTime).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      assertThat(it.suspendedBy).isEqualTo("Activities Management Service")
    }

    // Attendance one should be untouched
    with(attendanceRepository.findById(1L).orElseThrow()) {
      assertThat(status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(attendanceReason).isNull()
      assertThat(issuePayment).isNull()
      assertThat(recordedTime).isNull()
      assertThat(recordedBy).isNull()
    }

    // Attendance two should be suspended
    attendanceRepository.findAllById(listOf(2L, 3L)).onEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
      assertThat(it.attendanceReason?.code).isEqualTo(AttendanceReasonEnum.SUSPENDED)
      assertThat(it.issuePayment).isFalse()
      assertThat(it.recordedTime).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      assertThat(it.recordedBy).isEqualTo("Activities Management Service")
    }

    // Four events should be raised two for allocation amendments and two for an attendance amendment
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 2L)
    verify(outboundEventsService, never()).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 2L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 3L)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-changed-event.sql")
  fun `two allocations and two future attendance are unsuspended on receipt of offender received event for prisoner`() {
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber = "A11111A", fullInfo = true)

    assertThatAllocationsAreActiveFor(pentonvillePrisonCode, "A11111A")

    // Suspending first so can unspend afterwards.
    service.process(activitiesChangedEvent(prisonId = pentonvillePrisonCode, prisonerNumber = "A11111A", action = Action.SUSPEND))

    service.process(offenderReceivedFromTemporaryAbsence(prisonCode = pentonvillePrisonCode, prisonerNumber = "A11111A"))

    // Eight events should be raised four for allocation amendments and four for an attendance amendment
    verify(outboundEventsService, times(2)).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService, times(2)).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 2L)
    verify(outboundEventsService, never()).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 1L)
    verify(outboundEventsService, times(2)).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 2L)
    verify(outboundEventsService, times(2)).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 3L)

    // This attendance record is never modified
    attendanceRepository.findById(1L).orElseThrow().also {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(it.attendanceReason).isNull()
      assertThat(it.issuePayment).isNull()
      assertThat(it.recordedTime).isNull()
      assertThat(it.recordedBy).isNull()
    }

    // These attendance records are modified and now WAITING
    attendanceRepository.findAllById(listOf(2L, 3L)).onEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(it.attendanceReason).isNull()
      assertThat(it.issuePayment).isNull()
      assertThat(it.recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(it.recordedBy).isEqualTo("Activities Management Service")
    }
  }

  @Test
  @Sql("classpath:test_data/seed-offender-received-event-cancel-suspensions.sql")
  fun `two suspended allocations are unsuspended and two future attendance are cancelled on receipt of offender received event for prisoner`() {
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber = "A11111A", fullInfo = true)

    allocationRepository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.AUTO_SUSPENDED)).isTrue
    }

    attendanceRepository.findById(1L).orElseThrow().also {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(it.attendanceReason).isNull()
      assertThat(it.issuePayment).isNull()
      assertThat(it.recordedTime).isNull()
      assertThat(it.recordedBy).isNull()
    }

    attendanceRepository.findAllById(listOf(2L, 3L)).onEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
      assertThat(it.attendanceReason?.code).isEqualTo(AttendanceReasonEnum.SUSPENDED)
      assertThat(it.issuePayment).isFalse()
      assertThat(it.recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(it.recordedBy).isEqualTo("Activities Management Service")
    }

    service.process(offenderReceivedFromTemporaryAbsence(prisonCode = pentonvillePrisonCode, prisonerNumber = "A11111A"))

    attendanceRepository.findById(1L).orElseThrow().also {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(it.attendanceReason).isNull()
      assertThat(it.issuePayment).isNull()
      assertThat(it.recordedTime).isNull()
      assertThat(it.recordedBy).isNull()
    }

    attendanceRepository.findAllById(listOf(2L, 3L)).onEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
      assertThat(it.attendanceReason?.code).isEqualTo(AttendanceReasonEnum.CANCELLED)
      assertThat(it.issuePayment).isTrue()
      assertThat(it.recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(it.recordedBy).isEqualTo("Activities Management Service")
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 2L)
    verify(outboundEventsService, never()).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 2L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ATTENDANCE_AMENDED, 3L)
  }

  @Test
  @Sql("classpath:test_data/seed-activities-changed-event-deletes-attendances.sql")
  fun `allocations are ended and future attendances are removed on receipt of activities changed event for prisoner`() {
    assertThatAllocationsAreActiveFor(pentonvillePrisonCode, "A11111A")

    assertThat(attendanceRepository.findAllById(listOf(1L, 2L, 3L)).map { it.attendanceId }).containsExactlyInAnyOrder(1L, 2L, 3L)

    service.process(activitiesChangedEvent(prisonId = pentonvillePrisonCode, prisonerNumber = "A11111A", action = Action.END))

    assertThatAllocationsAreEndedFor(pentonvillePrisonCode, "A11111A", DeallocationReason.TEMPORARY_ABSENCE)

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 2L)

    assertThat(attendanceRepository.findAllById(listOf(1L, 2L, 3L)).map { it.attendanceId }).containsOnly(1L)
  }

  private fun assertThatAllocationsAreActiveFor(prisonCode: String, prisonerNumber: String) {
    allocationRepository.findByPrisonCodeAndPrisonerNumber(prisonCode, prisonerNumber).onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue
    }
  }

  private fun assertThatAllocationsAreEndedFor(prisonCode: String, prisonerNumber: String, reasonOverride: DeallocationReason = DeallocationReason.RELEASED) {
    allocationRepository.findByPrisonCodeAndPrisonerNumber(prisonCode, prisonerNumber).onEach {
      assertThat(it.status(PrisonerStatus.ENDED)).isTrue
      assertThat(it.deallocatedReason).isEqualTo(reasonOverride)
    }
  }

  private fun assertThatWaitingListStatusIs(status: WaitingListStatus, prisonCode: String, vararg prisonerNumbers: String) {
    waitingListRepository.findByPrisonCodeAndPrisonerNumberIn(prisonCode, prisonerNumbers.asList().toSet()).onEach {
      assertThat(it.status).isEqualTo(status)
    }
  }
}
