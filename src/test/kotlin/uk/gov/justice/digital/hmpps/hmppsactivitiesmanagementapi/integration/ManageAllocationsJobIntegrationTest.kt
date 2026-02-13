package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus.ACTIVE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus.AUTO_SUSPENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus.PENDING
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus.SUSPENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isNotEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ALLOCATION_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_AMENDED
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

@Deprecated("Remove when allocations job always uses SQS")
@TestPropertySource(
  properties = [
    "feature.jobs.sqs.activate.allocations.enabled=false",
    "feature.jobs.sqs.manage.attendances.enabled=false",
    "feature.jobs.sqs.manage.appointment.attendees.enabled=false",
  ],
)
class ManageAllocationsJobIntegrationTest : LocalStackTestBase() {

  @MockitoBean
  private lateinit var clock: Clock

  @Autowired
  private lateinit var allocationRepository: AllocationRepository

  @Autowired
  private lateinit var waitingListRepository: WaitingListRepository

  private val hmppsAuditEventCaptor = argumentCaptor<HmppsAuditEvent>()

  @BeforeEach
  fun beforeEach() {
    whenever(clock.instant()).thenReturn(LocalDateTime.now().toInstant(ZoneOffset.UTC))
    whenever(clock.zone).thenReturn(ZoneId.of("UTC"))
  }

  @Sql("classpath:test_data/seed-allocations-pending.sql")
  @Test
  fun `pending allocations on or before today are activated when prisoners are in prison`() {
    listOf("PAST", "TODAY", "FUTURE").map { prisonerNumber ->
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = prisonerNumber,
        prisonId = "PVI",
      )
    }.also { prisoners -> prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf("PAST", "TODAY"), prisoners) }

    with(allocationRepository.findAll()) {
      size isEqualTo 3
      prisoner("PAST") isStatus PENDING
      prisoner("TODAY") isStatus PENDING
      prisoner("FUTURE") isStatus PENDING
    }

    waitForJobs({ webTestClient.manageAllocations(withActivate = true) }, 3)

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 3),
    )

    with(allocationRepository.findAll()) {
      prisoner("PAST") isStatus ACTIVE
      prisoner("TODAY") isStatus ACTIVE
      prisoner("FUTURE") isStatus PENDING
    }
  }

  @Sql("classpath:test_data/seed-allocations-pending.sql")
  @Test
  fun `pending allocations on or before today are suspended when prisoners are out of prison`() {
    listOf("PAST", "TODAY").map { prisonerNumber ->
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = prisonerNumber,
        inOutStatus = Prisoner.InOutStatus.OUT,
      )
    }.also { prisoners -> prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf("PAST", "TODAY"), prisoners) }

    with(allocationRepository.findAll()) {
      size isEqualTo 3
      prisoner("PAST") isStatus PENDING
      prisoner("TODAY") isStatus PENDING
      prisoner("FUTURE") isStatus PENDING
    }

    waitForJobs({ webTestClient.manageAllocations(withActivate = true) }, 3)

    with(allocationRepository.findAll()) {
      prisoner("PAST") isStatus AUTO_SUSPENDED
      prisoner("TODAY") isStatus AUTO_SUSPENDED
      prisoner("FUTURE") isStatus PENDING
    }
  }

  @Sql("classpath:test_data/seed-allocations-with-planned-suspension.sql")
  @Test
  fun `active and pending allocations on or before today are suspended when they have a planned suspension`() {
    listOf("TODAY").map { prisonerNumber ->
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = prisonerNumber,
        prisonId = "PVI",
      )
    }.also { prisoners -> prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf("TODAY"), prisoners) }

    with(allocationRepository.findAll()) {
      size isEqualTo 3
      prisoner("PAST") isStatus ACTIVE
      prisoner("TODAY") isStatus PENDING
      prisoner("FUTURE") isStatus PENDING
    }

    waitForJobs({ webTestClient.manageAllocations(withActivate = true) }, 3)

    with(allocationRepository.findAll()) {
      prisoner("PAST") isStatus SUSPENDED
      prisoner("TODAY") isStatus SUSPENDED
      prisoner("FUTURE") isStatus PENDING
    }
  }

  @Sql("classpath:test_data/seed-allocation-with-planned-suspension-ending-today.sql")
  @Test
  fun `suspended allocations which are planned to be un-suspended today are set to ACTIVE`() {
    with(allocationRepository.findAll()) {
      size isEqualTo 1
      prisoner("G4508UU") isStatus SUSPENDED
    }

    waitForJobs({ webTestClient.manageAllocations(withActivate = true) }, 3)

    with(allocationRepository.findAll()) {
      prisoner("G4508UU") isStatus ACTIVE
    }
  }

  @Sql("classpath:test_data/fix-auto-suspend/prisoner-is-not-manually-suspended.sql")
  @Test
  fun `fix prisoners who are incorrectly auto-suspended`() {
    val prisoner = PrisonerSearchPrisonerFixture.instance(
      prisonId = "PVI",
      prisonerNumber = "A11111A",
      status = "ACTIVE IN",
    )

    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisoner)

    with(webTestClient.getAllocation(1)!!) {
      status isEqualTo AUTO_SUSPENDED
      suspendedTime isNotEqualTo null
      suspendedReason isNotEqualTo null
      suspendedBy isNotEqualTo null
    }

    with(webTestClient.getAttendanceById(1)!!) {
      status isEqualTo AttendanceStatus.COMPLETED.toString()
      attendanceReason!!.code isEqualTo AttendanceReasonEnum.AUTO_SUSPENDED.toString()
      issuePayment isEqualTo false
    }

    waitForJobs({ webTestClient.manageAllocations(withFixAutoSuspended = true) })

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_AMENDED, 1),
    )

    with(webTestClient.getAllocation(1)!!) {
      status isEqualTo ACTIVE
      suspendedTime isEqualTo null
      suspendedReason isEqualTo null
      suspendedBy isEqualTo null
    }

    with(webTestClient.getAttendanceById(1)!!) {
      status isEqualTo AttendanceStatus.WAITING.toString()
      attendanceReason isEqualTo null
      issuePayment isEqualTo null
    }
  }

  @Sql("classpath:test_data/fix-auto-suspend/prisoner-is-manually-suspended.sql")
  @Test
  fun `fix prisoners who are incorrectly auto-suspended but should be suspended`() {
    val prisoner = PrisonerSearchPrisonerFixture.instance(
      prisonId = "PVI",
      prisonerNumber = "A11111A",
      status = "ACTIVE IN",
    )

    prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisoner)

    with(webTestClient.getAllocation(1)!!) {
      status isEqualTo AUTO_SUSPENDED
      suspendedTime isNotEqualTo null
      suspendedReason isNotEqualTo null
      suspendedBy isNotEqualTo null
    }

    with(webTestClient.getAttendanceById(1)!!) {
      status isEqualTo AttendanceStatus.COMPLETED.toString()
      attendanceReason!!.code isEqualTo AttendanceReasonEnum.AUTO_SUSPENDED.toString()
      issuePayment isEqualTo false
    }

    waitForJobs({ webTestClient.manageAllocations(withFixAutoSuspended = true) })

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_AMENDED, 1),
    )

    with(webTestClient.getAllocation(1)!!) {
      status isEqualTo SUSPENDED
      suspendedTime isCloseTo LocalDateTime.now()
      suspendedReason isEqualTo "Planned suspension"
      suspendedBy isEqualTo "MRS BLOGS"
    }

    with(webTestClient.getAttendanceById(1)!!) {
      status isEqualTo AttendanceStatus.COMPLETED.toString()
      attendanceReason!!.code isEqualTo AttendanceReasonEnum.SUSPENDED.toString()
      issuePayment isEqualTo false
    }
  }

  @Sql("classpath:test_data/fix-auto-suspend/prisoners-correctly-auto-suspended.sql")
  @Test
  fun `do not fix prisoners who are correctly auto-suspended`() {
    val prisonerNumbers = listOf("A11111A", "B22222B", "C33333C")

    prisonerNumbers.forEach { prisonerNumber ->
      val prisoner = PrisonerSearchPrisonerFixture.instance(
        prisonId = "PVI",
        prisonerNumber = prisonerNumber,
        status = "ACTIVE IN",
      )

      prisonerSearchApiMockServer.stubSearchByPrisonerNumber(prisoner)
    }

    waitForJobs({ webTestClient.manageAllocations(withFixAutoSuspended = true) })

    validateNoMessagesSent()

    (1L..3L).forEach { allocationId ->
      webTestClient.getAllocation(allocationId)!!.status isEqualTo AUTO_SUSPENDED
    }
  }

  private infix fun WaitingListStatus.isStatus(status: WaitingListStatus) {
    this isEqualTo status
  }

  private fun List<Allocation>.prisoner(number: String) = single { it.prisonerNumber == number }

  private fun List<WaitingList>.prisoner(number: String) = single { it.prisonerNumber == number }

  private infix fun Allocation.isStatus(status: PrisonerStatus) {
    assertThat(this.prisonerStatus).isEqualTo(status)
  }

  private fun WebTestClient.manageAllocations(withActivate: Boolean = false, withFixAutoSuspended: Boolean = false) {
    post()
      .uri("/job/manage-allocations?withActivate=$withActivate&withFixAutoSuspended=$withFixAutoSuspended")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated
  }
}
