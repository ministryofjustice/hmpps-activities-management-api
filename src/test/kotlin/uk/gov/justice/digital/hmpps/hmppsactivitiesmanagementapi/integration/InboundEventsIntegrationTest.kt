package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
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
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isNotEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentAttendeeSearchRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.appointment.AppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.InmateDetailFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeInLiverpoolInmate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeInMoorlandInmate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeInMoorlandPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeInPentonvilleInmate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.activeInPentonvillePrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.convert
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.Action
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_DELETED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ALLOCATION_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_DELETED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ReleaseInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.SQSMessage
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.activitiesChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.alertsUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.appointmentsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderMergedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.prisonerReceivedFromTemporaryAbsence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.prisonerReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.permanentlyReleasedPrisonerToday
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * This integration test is bypassing the step whereby this would be instigated by incoming prisoner events.
 */
@TestPropertySource(
  properties = [
    "feature.audit.service.hmpps.enabled=true",
    "feature.audit.service.local.enabled=true",
    "feature.offender.merge.enabled=true",
    "feature.event.prison-offender-events.prisoner.merged=true",
  ],
)
class InboundEventsIntegrationTest : LocalStackTestBase() {

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
  private lateinit var appointmentRepository: AppointmentRepository

  @Autowired
  private lateinit var appointmentAttendeeRepository: AppointmentAttendeeRepository

  @Autowired
  private lateinit var appointmentAttendeeSearchRepository: AppointmentAttendeeSearchRepository

  @Autowired
  private lateinit var auditRepository: AuditRepository

  private val hmppsAuditEventCaptor = argumentCaptor<HmppsAuditEvent>()

  @Autowired
  private lateinit var service: InboundEventsService

  @BeforeEach
  fun initMocks() {
    reset(outboundEventsService)

    prisonApiMockServer.resetAll()
    prisonerSearchApiMockServer.resetAll()
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `release with unknown reason does not affect allocations but is treated as an interesting event`() {
    assertThat(eventReviewRepository.count()).isEqualTo(0)

    stubPrisonerForInterestingEvent("A11111A")

    assertThatAllocationsAreActiveFor("A11111A")

    // This event falls back to being processed as an interesting event due to the unknown reason for release
    service.process(
      PrisonerReleasedEvent(
        ReleaseInformation(
          nomsNumber = "A11111A",
          reason = "UNKNOWN",
          prisonId = PENTONVILLE_PRISON_CODE,
        ),
      ),
    )

    assertThatAllocationsAreActiveFor("A11111A")

    assertThat(eventReviewRepository.count()).isEqualTo(1L)
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  @Sql("classpath:test_data/seed-offender-with-waiting-list-application.sql")
  fun `permanent release of prisoner removes waiting list applications for offender`() {
    // Fixture necessary for the release event handler
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(permanentlyReleasedPrisonerToday)

    stubPrisonerForInterestingEvent("A11111A")

    assertThatWaitingListStatusIs(WaitingListStatus.PENDING, PENTONVILLE_PRISON_CODE, "A11111A")

    service.process(prisonerReleasedEvent(prisonerNumber = "A11111A"))

    assertThatWaitingListStatusIs(WaitingListStatus.REMOVED, PENTONVILLE_PRISON_CODE, "A11111A")

    verify(hmppsAuditApiClient, times(1)).createEvent(hmppsAuditEventCaptor.capture())

    hmppsAuditEventCaptor.firstValue.what isEqualTo "PRISONER_REMOVED_FROM_WAITING_LIST"
  }

  @Test
  @Sql("classpath:test_data/seed-offender-for-release.sql")
  fun `permanent release of prisoner ends allocations, removes waiting list applications and deletes pending allocations for offender`() {
    // Fixture necessary for the release event handler
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(permanentlyReleasedPrisonerToday.copy(prisonerNumber = "A11111A"))

    stubPrisonerForInterestingEvent("A11111A")

    assertThatWaitingListStatusIs(WaitingListStatus.PENDING, PENTONVILLE_PRISON_CODE, "A11111A")

    with(allocationRepository.findAll().filter { it.prisonerNumber == "A11111A" }) {
      size isEqualTo 3
      single { it.allocationId == 1L }.prisonerStatus isEqualTo PrisonerStatus.ACTIVE
      single { it.allocationId == 4L }.prisonerStatus isEqualTo PrisonerStatus.ACTIVE
      single { it.allocationId == 6L }.prisonerStatus isEqualTo PrisonerStatus.PENDING
    }

    service.process(prisonerReleasedEvent(prisonerNumber = "A11111A"))

    with(allocationRepository.findAll().filter { it.prisonerNumber == "A11111A" }) {
      size isEqualTo 3
      single { it.allocationId == 1L }.prisonerStatus isEqualTo PrisonerStatus.ENDED
      single { it.allocationId == 4L }.prisonerStatus isEqualTo PrisonerStatus.ENDED
      single { it.allocationId == 6L }.let { allocation ->
        allocation.prisonerStatus isEqualTo PrisonerStatus.ENDED
        allocation.startDate isEqualTo TimeSource.tomorrow()
        allocation.endDate isEqualTo TimeSource.today()
      }
    }

    assertThatWaitingListStatusIs(WaitingListStatus.REMOVED, PENTONVILLE_PRISON_CODE, "A11111A")

    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 4L)
    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 6L)
    verifyNoMoreInteractions(outboundEventsService)

    verify(hmppsAuditApiClient, times(4)).createEvent(hmppsAuditEventCaptor.capture())

    hmppsAuditEventCaptor.firstValue.what isEqualTo "PRISONER_REMOVED_FROM_WAITING_LIST"
    hmppsAuditEventCaptor.secondValue.what isEqualTo "PRISONER_DEALLOCATED"
    hmppsAuditEventCaptor.thirdValue.what isEqualTo "PRISONER_DEALLOCATED"
    hmppsAuditEventCaptor.lastValue.what isEqualTo "PRISONER_DEALLOCATED"
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `prisoner alerts updated`() {
    stubPrisonerForInterestingEvent(prisoner = activeInPentonvilleInmate.copy(offenderNo = "A11111A"))

    service.process(alertsUpdatedEvent(prisonerNumber = "A11111A"))

    val interestingEvent = eventReviewRepository.findAll().last()

    assertThat(interestingEvent.eventType).isEqualTo("prisoner-offender-search.prisoner.alerts-updated")
    assertThat(interestingEvent.prisonerNumber).isEqualTo("A11111A")
    assertThat(interestingEvent.eventData).isEqualTo("Alerts updated")
  }

  @Test
  @Sql("classpath:test_data/seed-offender-released-event-deletes-attendances.sql")
  fun `permanent release of prisoner removes any unattended future attendances at time of event being raised`() {
    // Fixture necessary for the release event handler
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(permanentlyReleasedPrisonerToday.copy(prisonerNumber = "A11111A"))

    assertThatAllocationsAreActiveFor("A11111A")

    assertThat(attendanceRepository.findAllById(listOf(1L, 2L, 3L, 4L, 5L)).map { it.attendanceId }).containsExactlyInAnyOrder(
      1L,
      2L,
      3L,
      4L,
      5L,
    )

    service.process(prisonerReleasedEvent(prisonerNumber = "A11111A"))

    assertThatAllocationsAreEndedFor(PENTONVILLE_PRISON_CODE, "A11111A")

    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 2L)
    verify(outboundEventsService).send(PRISONER_ATTENDANCE_DELETED, 10001, 3L)
    verify(outboundEventsService).send(PRISONER_ATTENDANCE_DELETED, 10021, 2L)
    verifyNoMoreInteractions(outboundEventsService)

    assertThat(attendanceRepository.findAllById(listOf(1L, 2L, 3L, 4L, 5L)).map { it.attendanceId }).containsOnly(1L, 4L, 5L)
  }

  @Test
  @Sql("classpath:test_data/seed-appointments-changed-event.sql")
  fun `appointments deleted when appointments changed event received with action set to YES`() {
    val appointmentIds = listOf(200L, 201L, 202L, 203L, 210L, 211L, 212L)

    stubPrisonerForInterestingEvent(activeInMoorlandInmate.copy(offenderNo = "A1234BC"))

    allocationRepository.findAll().filter { it.prisonerNumber == "A11111A" }.onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue
    }

    var allocationsMap = appointmentAttendeeSearchRepository.findByAppointmentIds(
      appointmentIds,
    ).groupBy { it.appointmentSearch.appointmentId }

    assertThat(allocationsMap[200]).hasSize(1)
    assertThat(allocationsMap[201]).hasSize(1)
    assertThat(allocationsMap[202]).hasSize(1)
    assertThat(allocationsMap[203]).hasSize(1)
    assertThat(allocationsMap[210]).hasSize(2)
    assertThat(allocationsMap[211]).hasSize(3)
    assertThat(allocationsMap[212]).hasSize(2)

    service.process(appointmentsChangedEvent(prisonerNumber = "A1234BC"))

    allocationsMap = appointmentAttendeeSearchRepository.findByAppointmentIds(
      appointmentIds,
    ).groupBy { it.appointmentSearch.appointmentId }

    assertThat(allocationsMap[200]).isNull()
    assertThat(allocationsMap[201]).hasSize(1)
    assertThat(allocationsMap[202]).isNull()
    assertThat(allocationsMap[203]).hasSize(1)
    assertThat(allocationsMap[210]).hasSize(2)
    assertThat(allocationsMap[211]).hasSize(2)
    assertThat(allocationsMap[212]).hasSize(1)

    assertThat(appointmentAttendeeRepository.existsById(300)).isFalse()
    assertThat(appointmentAttendeeRepository.existsById(302)).isFalse()
    assertThat(appointmentAttendeeRepository.existsById(322)).isFalse()
    assertThat(appointmentAttendeeRepository.existsById(324)).isFalse()

    assertThat(appointmentAttendeeRepository.existsById(301)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(303)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(320)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(321)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(323)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(325)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(326)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(327)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(328)).isTrue()

    verify(outboundEventsService).send(APPOINTMENT_INSTANCE_DELETED, 300L)
    verify(outboundEventsService).send(APPOINTMENT_INSTANCE_DELETED, 302L)
    verify(outboundEventsService).send(APPOINTMENT_INSTANCE_DELETED, 322L)
    verify(outboundEventsService).send(APPOINTMENT_INSTANCE_DELETED, 324L)
    verifyNoMoreInteractions(outboundEventsService)

    verify(hmppsAuditApiClient, times(4)).createEvent(hmppsAuditEventCaptor.capture())
    hmppsAuditEventCaptor.allValues.map { it.what }.distinct()
      .single() isEqualTo AuditEventType.APPOINTMENT_CANCELLED_ON_TRANSFER.toString()
    verifyNoMoreInteractions(hmppsAuditApiClient)
  }

  @Test
  @Sql("classpath:test_data/seed-appointments-changed-event.sql")
  fun `appointments deleted when offender released event received`() {
    val appointmentIds = listOf(200L, 201L, 202L, 203L, 210L, 211L, 212L)

    stubPrisonerForInterestingEvent(activeInMoorlandInmate.copy(offenderNo = "A1234BC"))

    allocationRepository.findAll().filter { it.prisonerNumber == "A11111A" }.onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE)).isTrue
    }

    var allocationsMap = appointmentAttendeeSearchRepository.findByAppointmentIds(
      appointmentIds,
    ).groupBy { it.appointmentSearch.appointmentId }

    assertThat(allocationsMap[200]).hasSize(1)
    assertThat(allocationsMap[201]).hasSize(1)
    assertThat(allocationsMap[202]).hasSize(1)
    assertThat(allocationsMap[203]).hasSize(1)
    assertThat(allocationsMap[210]).hasSize(2)
    assertThat(allocationsMap[211]).hasSize(3)
    assertThat(allocationsMap[212]).hasSize(2)

    service.process(prisonerReleasedEvent(prisonerNumber = "A1234BC", prisonCode = "MDI"))

    allocationsMap = appointmentAttendeeSearchRepository.findByAppointmentIds(
      appointmentIds,
    ).groupBy { it.appointmentSearch.appointmentId }

    assertThat(allocationsMap[200]).isNull()
    assertThat(allocationsMap[201]).hasSize(1)
    assertThat(allocationsMap[202]).isNull()
    assertThat(allocationsMap[203]).hasSize(1)
    assertThat(allocationsMap[210]).hasSize(2)
    assertThat(allocationsMap[211]).hasSize(2)
    assertThat(allocationsMap[212]).hasSize(1)

    assertThat(appointmentAttendeeRepository.existsById(300)).isFalse()
    assertThat(appointmentAttendeeRepository.existsById(302)).isFalse()
    assertThat(appointmentAttendeeRepository.existsById(322)).isFalse()
    assertThat(appointmentAttendeeRepository.existsById(324)).isFalse()

    assertThat(appointmentAttendeeRepository.existsById(301)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(303)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(320)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(321)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(323)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(325)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(326)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(327)).isTrue()
    assertThat(appointmentAttendeeRepository.existsById(328)).isTrue()

    verify(outboundEventsService).send(APPOINTMENT_INSTANCE_DELETED, 300L)
    verify(outboundEventsService).send(APPOINTMENT_INSTANCE_DELETED, 302L)
    verify(outboundEventsService).send(APPOINTMENT_INSTANCE_DELETED, 322L)
    verify(outboundEventsService).send(APPOINTMENT_INSTANCE_DELETED, 324L)
    verifyNoMoreInteractions(outboundEventsService)

    verify(hmppsAuditApiClient, times(4)).createEvent(hmppsAuditEventCaptor.capture())
    hmppsAuditEventCaptor.allValues.map { it.what }.distinct()
      .single() isEqualTo AuditEventType.APPOINTMENT_CANCELLED_ON_TRANSFER.toString()
    verifyNoMoreInteractions(hmppsAuditApiClient)
  }

  @Test
  @Sql("classpath:test_data/seed-appointments-changed-event.sql")
  fun `no appointments are cancelled when appointments changed event received with action set to NO`() {
    val appointmentIds = listOf(200L, 201L, 202L, 203L, 210L, 211L, 212L)
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(permanentlyReleasedPrisonerToday.copy(prisonerNumber = "A1234BC"))

    stubPrisonerForInterestingEvent(
      InmateDetailFixture.instance(
        offenderNo = "A1234BC",
        agencyId = MOORLAND_PRISON_CODE,
      ),
    )

    allocationRepository.findAll().filter { it.prisonerNumber == "A11111A" }.onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    var allocationsMap = appointmentAttendeeSearchRepository.findByAppointmentIds(
      appointmentIds,
    ).groupBy { it.appointmentSearch.appointmentId }

    assertThat(allocationsMap[200]).hasSize(1)
    assertThat(allocationsMap[201]).hasSize(1)
    assertThat(allocationsMap[202]).hasSize(1)
    assertThat(allocationsMap[203]).hasSize(1)
    assertThat(allocationsMap[210]).hasSize(2)
    assertThat(allocationsMap[211]).hasSize(3)
    assertThat(allocationsMap[212]).hasSize(2)

    service.process(appointmentsChangedEvent(prisonerNumber = "A1234BC", action = "NO"))

    allocationsMap = appointmentAttendeeSearchRepository.findByAppointmentIds(
      appointmentIds,
    ).groupBy { it.appointmentSearch.appointmentId }

    assertThat(allocationsMap[200]).hasSize(1)
    assertThat(allocationsMap[201]).hasSize(1)
    assertThat(allocationsMap[202]).hasSize(1)
    assertThat(allocationsMap[203]).hasSize(1)
    assertThat(allocationsMap[210]).hasSize(2)
    assertThat(allocationsMap[211]).hasSize(3)
    assertThat(allocationsMap[212]).hasSize(2)

    assertThat(appointmentRepository.existsById(200)).isTrue()
    assertThat(appointmentRepository.existsById(201)).isTrue()
    assertThat(appointmentRepository.existsById(202)).isTrue()
    assertThat(appointmentRepository.existsById(203)).isTrue()
    assertThat(appointmentRepository.existsById(210)).isTrue()
    assertThat(appointmentRepository.existsById(211)).isTrue()
    assertThat(appointmentRepository.existsById(212)).isTrue()

    verifyNoInteractions(outboundEventsService)
    verifyNoInteractions(hmppsAuditApiClient)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-changed-event.sql")
  fun `two allocations and two future attendance are auto-suspended on receipt of activities changed event for prisoner`() {
    stubPrisonerForInterestingEvent(activeInPentonvilleInmate.copy(offenderNo = "A11111A"))

    assertThatAllocationsAreActiveFor("A11111A")

    attendanceRepository.findAllById(listOf(1L, 2L, 3L)).onEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(it.attendanceReason).isNull()
      assertThat(it.issuePayment).isNull()
      assertThat(it.recordedTime).isNull()
      assertThat(it.recordedBy).isNull()
    }

    service.process(
      activitiesChangedEvent(
        prisonId = PENTONVILLE_PRISON_CODE,
        prisonerNumber = "A11111A",
        action = Action.SUSPEND,
      ),
    )

    allocationRepository.findByPrisonCodeAndPrisonerNumber(PENTONVILLE_PRISON_CODE, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.AUTO_SUSPENDED))
      assertThat(it.suspendedReason).isEqualTo("Temporarily released or transferred")
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
      assertThat(it.attendanceReason?.code).isEqualTo(AttendanceReasonEnum.AUTO_SUSPENDED)
      assertThat(it.issuePayment).isFalse()
      assertThat(it.recordedTime).isCloseTo(LocalDateTime.now(), within(10, ChronoUnit.SECONDS))
      assertThat(it.recordedBy).isEqualTo("Activities Management Service")
    }

    // Four events should be raised two for allocation amendments and two for an attendance amendment
    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 2L)
    verify(outboundEventsService, never()).send(PRISONER_ATTENDANCE_AMENDED, 1L)
    verify(outboundEventsService).send(PRISONER_ATTENDANCE_AMENDED, 2L)
    verify(outboundEventsService).send(PRISONER_ATTENDANCE_AMENDED, 3L)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-changed-event.sql")
  fun `two allocations and two future attendance are unsuspended on receipt of offender received event`() {
    // Fixture necessary for the received event handler
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInPentonvillePrisoner.copy(prisonerNumber = "A11111A"))

    assertThatAllocationsAreActiveFor("A11111A")

    // Suspending first so can unsuspend afterwards.
    service.process(
      activitiesChangedEvent(
        prisonId = PENTONVILLE_PRISON_CODE,
        prisonerNumber = "A11111A",
        action = Action.SUSPEND,
      ),
    )

    service.process(
      prisonerReceivedFromTemporaryAbsence(
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerNumber = "A11111A",
      ),
    )

    // Eight events should be raised four for allocation amendments and four for an attendance amendment
    verify(outboundEventsService, times(2)).send(PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService, times(2)).send(PRISONER_ALLOCATION_AMENDED, 2L)
    verify(outboundEventsService, never()).send(PRISONER_ATTENDANCE_AMENDED, 1L)
    verify(outboundEventsService, times(2)).send(PRISONER_ATTENDANCE_AMENDED, 2L)
    verify(outboundEventsService, times(2)).send(PRISONER_ATTENDANCE_AMENDED, 3L)
    verifyNoMoreInteractions(outboundEventsService)

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
  @Sql("classpath:test_data/seed-activity-changed-event.sql")
  fun `two allocations and two future attendance are unsuspended on receipt of prisoner received event`() {
    // Fixture necessary for the received event handler
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInPentonvillePrisoner.copy(prisonerNumber = "A11111A"))

    assertThatAllocationsAreActiveFor("A11111A")

    // Suspending first so can unsuspend afterwards.
    service.process(
      activitiesChangedEvent(
        prisonId = PENTONVILLE_PRISON_CODE,
        prisonerNumber = "A11111A",
        action = Action.SUSPEND,
      ),
    )

    service.process(
      prisonerReceivedFromTemporaryAbsence(
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerNumber = "A11111A",
      ),
    )

    // Eight events should be raised four for allocation amendments and four for an attendance amendment
    verify(outboundEventsService, times(2)).send(PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService, times(2)).send(PRISONER_ALLOCATION_AMENDED, 2L)
    verify(outboundEventsService, never()).send(PRISONER_ATTENDANCE_AMENDED, 1L)
    verify(outboundEventsService, times(2)).send(PRISONER_ATTENDANCE_AMENDED, 2L)
    verify(outboundEventsService, times(2)).send(PRISONER_ATTENDANCE_AMENDED, 3L)
    verifyNoMoreInteractions(outboundEventsService)

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
  fun `two auto-suspended allocations are unsuspended and two future attendance are cancelled for offender received event`() {
    // Fixture necessary for the received event handler
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInPentonvillePrisoner.copy(prisonerNumber = "A11111A"))

    allocationRepository.findAll().filter { it.prisonerNumber == "A11111A" }.onEach {
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
      assertThat(it.attendanceReason?.code).isEqualTo(AttendanceReasonEnum.AUTO_SUSPENDED)
      assertThat(it.issuePayment).isFalse()
      assertThat(it.recordedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(it.recordedBy).isEqualTo("Activities Management Service")
    }

    service.process(
      prisonerReceivedFromTemporaryAbsence(
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerNumber = "A11111A",
      ),
    )

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

    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 2L)
    verify(outboundEventsService, never()).send(PRISONER_ATTENDANCE_AMENDED, 1L)
    verify(outboundEventsService).send(PRISONER_ATTENDANCE_AMENDED, 2L)
    verify(outboundEventsService).send(PRISONER_ATTENDANCE_AMENDED, 3L)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  @Sql("classpath:test_data/seed-offender-received-event-with-planned-suspension.sql")
  fun `auto-suspended allocation is suspended and future attendances are suspended when there is a planned suspension on receipt of offender received event`() {
    // Fixture necessary for the received event handler
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInPentonvillePrisoner.copy(prisonerNumber = "A11111A"))

    with(allocationRepository.getReferenceById(1L)) {
      status(PrisonerStatus.AUTO_SUSPENDED) isBool true
    }
    attendanceRepository.findAllById(listOf(1L, 2L)).onEach {
      it.status() isEqualTo AttendanceStatus.COMPLETED
      it.attendanceReason isNotEqualTo null
      it.attendanceReason!!.code isEqualTo AttendanceReasonEnum.AUTO_SUSPENDED
    }

    service.process(
      prisonerReceivedFromTemporaryAbsence(
        prisonCode = PENTONVILLE_PRISON_CODE,
        prisonerNumber = "A11111A",
      ),
    )

    with(allocationRepository.getReferenceById(1L)) {
      status(PrisonerStatus.SUSPENDED) isBool true
    }

    // This past attendance record has not been modified
    with(attendanceRepository.getReferenceById(1L)) {
      status() isEqualTo AttendanceStatus.COMPLETED
      attendanceReason isNotEqualTo null
      attendanceReason!!.code isEqualTo AttendanceReasonEnum.AUTO_SUSPENDED
    }

    // This future attendance record has been modified
    with(attendanceRepository.getReferenceById(2L)) {
      status() isEqualTo AttendanceStatus.COMPLETED
      attendanceReason isNotEqualTo null
      attendanceReason!!.code isEqualTo AttendanceReasonEnum.SUSPENDED
    }

    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService, never()).send(PRISONER_ATTENDANCE_AMENDED, 1L)
    verify(outboundEventsService).send(PRISONER_ATTENDANCE_AMENDED, 2L)
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  @Sql("classpath:test_data/seed-activities-changed-event-deletes-attendances.sql")
  fun `allocations are ended and future attendances are removed for activities changed event set to END on release`() {
    // Fixture necessary for the activities changed event handler
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(permanentlyReleasedPrisonerToday.copy(prisonerNumber = "A22222A"))

    assertThatAllocationsAreActiveFor("A22222A")

    assertThat(attendanceRepository.findAllById(listOf(1L, 2L, 3L)).map { it.attendanceId }).containsExactlyInAnyOrder(
      1L,
      2L,
      3L,
    )
    assertThatWaitingListStatusIs(WaitingListStatus.PENDING, PENTONVILLE_PRISON_CODE, "A22222A")

    service.process(
      activitiesChangedEvent(
        prisonId = PENTONVILLE_PRISON_CODE,
        prisonerNumber = "A22222A",
        action = Action.END,
      ),
    )

    assertThatAllocationsAreEndedFor(PENTONVILLE_PRISON_CODE, "A22222A", DeallocationReason.RELEASED)
    assertThatWaitingListStatusIs(WaitingListStatus.REMOVED, PENTONVILLE_PRISON_CODE, "A22222A")

    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 2L)
    verify(outboundEventsService).send(PRISONER_ATTENDANCE_DELETED, 10099, 2L)
    verify(outboundEventsService).send(PRISONER_ATTENDANCE_DELETED, 10001, 3L)
    verifyNoMoreInteractions(outboundEventsService)

    assertThat(attendanceRepository.findAllById(listOf(1L, 2L, 3L)).map { it.attendanceId }).containsOnly(1L)
  }

  @Test
  @Sql("classpath:test_data/seed-activities-changed-event-deletes-attendances.sql")
  fun `allocations are ended and future attendances are removed for activities changed event set to END on temporary release`() {
    // Fixture necessary for the activities changed event handler
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInMoorlandPrisoner.copy(prisonerNumber = "A22222A"))

    assertThatAllocationsAreActiveFor("A22222A")

    assertThat(attendanceRepository.findAllById(listOf(1L, 2L, 3L)).map { it.attendanceId }).containsExactlyInAnyOrder(
      1L,
      2L,
      3L,
    )
    assertThatWaitingListStatusIs(WaitingListStatus.PENDING, PENTONVILLE_PRISON_CODE, "A22222A")

    service.process(
      activitiesChangedEvent(
        prisonId = PENTONVILLE_PRISON_CODE,
        prisonerNumber = "A22222A",
        action = Action.END,
      ),
    )

    assertThatAllocationsAreEndedFor(PENTONVILLE_PRISON_CODE, "A22222A", DeallocationReason.TEMPORARILY_RELEASED)
    assertThatWaitingListStatusIs(WaitingListStatus.REMOVED, PENTONVILLE_PRISON_CODE, "A22222A")

    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(PRISONER_ALLOCATION_AMENDED, 2L)
    verify(outboundEventsService).send(PRISONER_ATTENDANCE_DELETED, 10099L, 2L)
    verify(outboundEventsService).send(PRISONER_ATTENDANCE_DELETED, 10001, 3L)
    verifyNoMoreInteractions(outboundEventsService)

    assertThat(attendanceRepository.findAllById(listOf(1L, 2L, 3L)).map { it.attendanceId }).containsOnly(1L)
  }

  private fun assertThatAllocationsAreActiveFor(prisonerNumber: String) {
    allocationRepository.findAll().filter { it.prisonerNumber == prisonerNumber }.onEach {
      it.prisonerStatus isEqualTo PrisonerStatus.ACTIVE
    }
  }

  private fun assertThatAllocationsAreEndedFor(
    prisonCode: String,
    prisonerNumber: String,
    reasonOverride: DeallocationReason = DeallocationReason.RELEASED,
  ) {
    allocationRepository.findByPrisonCodeAndPrisonerNumber(prisonCode, prisonerNumber).onEach {
      it.prisonerStatus isEqualTo PrisonerStatus.ENDED
      it.deallocatedReason isEqualTo reasonOverride
    }
  }

  private fun assertThatWaitingListStatusIs(status: WaitingListStatus, prisonCode: String, prisonerNumber: String) {
    waitingListRepository.findByPrisonCodeAndPrisonerNumber(prisonCode, prisonerNumber)
      .onEach {
        it.status isEqualTo status
      }
  }

  @Test
  @Sql("classpath:test_data/seed-offender-merged-event-old-and-new-allocations.sql")
  fun `offender merged event replaces old prisoner and new prisoner records with new prisoner number and booking ID for rolled out prison`() {
    val (oldPrisonerNumber, newPrisonerNumber) = "A11111A" to "B11111B"
    val (oldBookingId, newBookingId) = 111111L to 999999L
    val oldPrisonNumberAndOldBooking = oldPrisonerNumber to oldBookingId
    val newPrisonerNumberAndOldBooking = newPrisonerNumber to oldBookingId

    prisonApiMockServer.stubGetPrisonerDetails(activeInPentonvilleInmate.copy(offenderNo = newPrisonerNumber, bookingId = 999999))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInPentonvilleInmate.copy(offenderNo = newPrisonerNumber, bookingId = 999999).convert())

    // Check all set to the old prisoner number and booking ID before event is processed
    allocationRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(oldPrisonNumberAndOldBooking, newPrisonerNumberAndOldBooking)
    attendanceRepository.findAll().single().prisonerNumber isEqualTo oldPrisonerNumber
    waitingListRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(oldPrisonNumberAndOldBooking, newPrisonerNumberAndOldBooking)
    auditRepository.findAll().single().prisonerNumber isEqualTo oldPrisonerNumber
    eventReviewRepository.findAll().map { it.prisonerNumber to it.bookingId?.toLong() } containsExactlyInAnyOrder listOf(oldPrisonNumberAndOldBooking)
    appointmentAttendeeRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(oldPrisonNumberAndOldBooking, newPrisonerNumberAndOldBooking)

    val event = offenderMergedEvent(prisonerNumber = newPrisonerNumber, removedPrisonerNumber = oldPrisonerNumber)

    this.sendMessage(InboundEventType.OFFENDER_MERGED, event.additionalInformation)

    await untilAsserted {
      val newPrisonerNumberAndNewBooking = newPrisonerNumber to newBookingId

      // Check all set to the new prisoner number after event is processed
      allocationRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(
        newPrisonerNumberAndNewBooking,
        newPrisonerNumberAndNewBooking,
      )

      attendanceRepository.findAll().single().prisonerNumber isEqualTo newPrisonerNumber

      waitingListRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(
        newPrisonerNumberAndNewBooking,
        newPrisonerNumberAndNewBooking,
      )

      auditRepository.findAll().map { it.prisonerNumber }.all { it == newPrisonerNumber } isBool true

      eventReviewRepository.findAll()
        .map { it.prisonerNumber to it.bookingId?.toLong() } containsExactlyInAnyOrder listOf(
        newPrisonerNumberAndNewBooking,
        newPrisonerNumberAndNewBooking,
      )

      appointmentAttendeeRepository.findAll()
        .map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(
        newPrisonerNumberAndNewBooking,
        newPrisonerNumberAndNewBooking,
      )
    }
  }

  @Test
  @Sql("classpath:test_data/seed-offender-merged-event-old-and-new-allocations.sql")
  fun `offender merged event replaces old prisoner and new prisoner records with new prisoner number and booking ID for non-rolled out prison`() {
    val (oldPrisonerNumber, newPrisonerNumber) = "A11111A" to "B11111B"
    val (oldBookingId, newBookingId) = 111111L to 999999L
    val oldPrisonNumberAndOldBooking = oldPrisonerNumber to oldBookingId
    val newPrisonerNumberAndOldBooking = newPrisonerNumber to oldBookingId

    prisonApiMockServer.stubGetPrisonerDetails(activeInLiverpoolInmate.copy(offenderNo = newPrisonerNumber, bookingId = 999999))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInLiverpoolInmate.copy(offenderNo = newPrisonerNumber, bookingId = 999999).convert())

    // Check all set to the old prisoner number and booking ID before event is processed
    allocationRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(oldPrisonNumberAndOldBooking, newPrisonerNumberAndOldBooking)
    attendanceRepository.findAll().single().prisonerNumber isEqualTo oldPrisonerNumber
    waitingListRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(oldPrisonNumberAndOldBooking, newPrisonerNumberAndOldBooking)
    auditRepository.findAll().single().prisonerNumber isEqualTo oldPrisonerNumber
    eventReviewRepository.findAll().map { it.prisonerNumber to it.bookingId?.toLong() } containsExactlyInAnyOrder listOf(oldPrisonNumberAndOldBooking)
    appointmentAttendeeRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(oldPrisonNumberAndOldBooking, newPrisonerNumberAndOldBooking)

    service.process(offenderMergedEvent(prisonerNumber = newPrisonerNumber, removedPrisonerNumber = oldPrisonerNumber))

    val newPrisonerNumberAndNewBooking = newPrisonerNumber to newBookingId

    // Check all set to the new prisoner number after event is processed
    allocationRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(newPrisonerNumberAndNewBooking, newPrisonerNumberAndNewBooking)
    attendanceRepository.findAll().single().prisonerNumber isEqualTo newPrisonerNumber
    waitingListRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(newPrisonerNumberAndNewBooking, newPrisonerNumberAndNewBooking)
    auditRepository.findAll().map { it.prisonerNumber }.all { it == newPrisonerNumber } isBool true
    // New event will not be logged by InterestingEventHandler as it is not for rolled out prison
    eventReviewRepository.findAll().map { it.prisonerNumber to it.bookingId?.toLong() } containsExactlyInAnyOrder listOf(newPrisonerNumberAndNewBooking)
    appointmentAttendeeRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(newPrisonerNumberAndNewBooking, newPrisonerNumberAndNewBooking)
  }

  @Test
  @Sql("classpath:test_data/seed-offender-merged-event-old-allocations.sql")
  fun `offender merged event replaces old prisoner records with new prisoner number and booking ID`() {
    val (oldPrisonerNumber, newPrisonerNumber) = "A11111A" to "B11111B"
    val (oldBookingId, newBookingId) = 111111L to 999999L
    val oldPrisonNumberAndOldBooking = oldPrisonerNumber to oldBookingId

    prisonApiMockServer.stubGetPrisonerDetails(activeInPentonvilleInmate.copy(offenderNo = newPrisonerNumber, bookingId = 999999))
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(activeInPentonvilleInmate.copy(offenderNo = newPrisonerNumber, bookingId = 999999).convert())

    // Check all set to the old prisoner number and booking ID before event is processed
    allocationRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(oldPrisonNumberAndOldBooking)
    attendanceRepository.findAll().single().prisonerNumber isEqualTo oldPrisonerNumber
    waitingListRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(oldPrisonNumberAndOldBooking)
    auditRepository.findAll().single().prisonerNumber isEqualTo oldPrisonerNumber
    eventReviewRepository.findAll().map { it.prisonerNumber to it.bookingId?.toLong() } containsExactlyInAnyOrder listOf(oldPrisonNumberAndOldBooking)
    appointmentAttendeeRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(oldPrisonNumberAndOldBooking)

    service.process(offenderMergedEvent(prisonerNumber = newPrisonerNumber, removedPrisonerNumber = oldPrisonerNumber))

    val newPrisonerNumberAndNewBooking = newPrisonerNumber to newBookingId

    // Check all set to the new prisoner number after event is processed
    allocationRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(newPrisonerNumberAndNewBooking)
    attendanceRepository.findAll().single().prisonerNumber isEqualTo newPrisonerNumber
    waitingListRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(newPrisonerNumberAndNewBooking)
    auditRepository.findAll().map { it.prisonerNumber }.all { it == newPrisonerNumber } isBool true
    eventReviewRepository.findAll().map { it.prisonerNumber to it.bookingId?.toLong() } containsExactlyInAnyOrder listOf(newPrisonerNumberAndNewBooking, newPrisonerNumberAndNewBooking)
    appointmentAttendeeRepository.findAll().map { it.prisonerNumber to it.bookingId } containsExactlyInAnyOrder listOf(newPrisonerNumberAndNewBooking)
  }

  data class RawMessage(val eventType: String, val additionalInformation: Any)

  private fun sendMessage(eventType: InboundEventType, additionalInformation: Any) {
    val e = RawMessage(eventType.eventType, additionalInformation)

    val sqsMessage = SQSMessage("Notification", mapper.writeValueAsString(e))

    activitiesQueue.sqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl("activities")
        .messageBody(mapper.writeValueAsString(sqsMessage))
        .build(),
    )
  }
}
