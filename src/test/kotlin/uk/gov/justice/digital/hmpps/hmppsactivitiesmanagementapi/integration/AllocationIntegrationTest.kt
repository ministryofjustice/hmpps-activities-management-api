package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.daysFromNow
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isNotEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandOne
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandTwo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AllocationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.SuspendPrisonerRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.UnsuspendPrisonerRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_HUB
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ALLOCATION_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_AMENDED
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.PRISONER_ATTENDANCE_CREATED
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class AllocationIntegrationTest : LocalStackTestBase() {

  @Autowired
  private lateinit var allocationRepository: AllocationRepository

  @Autowired
  private lateinit var attendanceRepository: AttendanceRepository

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `get allocation by id`() {
    with(webTestClient.getAllocationBy(1)!!) {
      assertThat(prisonerNumber).isEqualTo("A11111A")
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandOne)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 9, 0))
    }

    with(webTestClient.getAllocationBy(2)!!) {
      assertThat(prisonerNumber).isEqualTo("A22222A")
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandTwo)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 9, 0))
    }
  }

  @Sql("classpath:test_data/seed-activity-with-active-exclusions.sql")
  @Test
  fun `get allocation - active exclusions are returned`() {
    with(webTestClient.getAllocationBy(2)!!) {
      exclusions hasSize 1
    }

    with(webTestClient.getAllocationBy(3)!!) {
      exclusions hasSize 0
    }
  }

  @Sql("classpath:test_data/seed-activity-with-future-exclusions.sql")
  @Test
  fun `get allocation - future exclusions are returned`() {
    with(webTestClient.getAllocationBy(2)!!) {
      exclusions hasSize 1
    }
  }

  @Sql("classpath:test_data/seed-activity-with-historical-exclusions.sql")
  @Test
  fun `get allocation - past exclusions are not returned`() {
    with(webTestClient.getAllocationBy(2)!!) {
      exclusions hasSize 0
    }
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `403 when attempting to get an allocation with the wrong case load ID`() {
    webTestClient.get()
      .uri("/allocations/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
      .header(CASELOAD_ID, "XXX")
      .exchange()
      .expectStatus().isForbidden
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `attempting to get an allocation without specifying a caseload succeeds if admin role present`() {
    webTestClient.get()
      .uri("/allocations/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = true, roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().isOk
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `attempting to get an allocation without specifying a caseload succeeds if the token is a client token`() {
    webTestClient.get()
      .uri("/allocations/id/2")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(isClientToken = true, roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().isOk
  }

  @Sql("classpath:test_data/seed-activity-id-20.sql")
  @Test
  fun `attempting to add waiting list to activity from a different caseload returns a 403`() {
    val request = WaitingListApplicationRequest(
      prisonerNumber = "G4793VF",
      activityScheduleId = 1L,
      applicationDate = TimeSource.today(),
      requestedBy = "Bob",
      comments = "Some comments from Bob",
      status = WaitingListStatus.PENDING,
    )

    webTestClient.waitingListApplication(MOORLAND_PRISON_CODE, request, PENTONVILLE_PRISON_CODE).expectStatus().isForbidden
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `update allocation start date to today`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumber("A1234BC")

    allocationRepository.findById(6).get().also {
      it.startDate isEqualTo TimeSource.today().plusDays(2)
      it.prisonerStatus isEqualTo PrisonerStatus.PENDING
    }

    webTestClient.updateAllocation(
      PENTONVILLE_PRISON_CODE,
      6,
      AllocationUpdateRequest(
        startDate = TimeSource.today(),
        scheduleInstanceId = 2L,
      ),
    )

    allocationRepository.findById(6).get().also {
      it.startDate isEqualTo TimeSource.today()
      it.prisonerStatus isEqualTo PrisonerStatus.ACTIVE
    }

    val newAttendance = attendanceRepository.findAll().filter { it.scheduledInstance.sessionDate == TimeSource.today() }
      .also { it.size isEqualTo 1 }
      .first()

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 6),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_CREATED, newAttendance.attendanceId),
    )
  }

  @Sql("classpath:test_data/seed-activity-with-advance-attendances-2.sql")
  @Test
  fun `update allocation start date to after instance with advance attendance`() {
    assertThat(webTestClient.getScheduledInstancesByIds(1)!!.first().advanceAttendances).extracting("prisonerNumber").contains("A11111A", "C33333C")

    webTestClient.updateAllocation(
      MOORLAND_PRISON_CODE,
      4,
      AllocationUpdateRequest(
        startDate = LocalDate.now().plusDays(2),
      ),
    )

    allocationRepository.findById(4).get().also {
      it.startDate isEqualTo LocalDate.now().plusDays(2)
      it.prisonerStatus isEqualTo PrisonerStatus.PENDING
    }

    assertThat(webTestClient.getScheduledInstancesByIds(1)!!.first().advanceAttendances).extracting("prisonerNumber").contains("A11111A")
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `update allocation end date`() {
    allocationRepository.findById(1).get().also { it.endDate isEqualTo null }

    prisonerSearchApiMockServer.stubSearchByPrisonerNumber("A11111A")

    assertThat(webTestClient.retrieveAdvanceAttendance(1).prisonerNumber).isEqualTo("A11111A")

    webTestClient.updateAllocation(
      PENTONVILLE_PRISON_CODE,
      1,
      AllocationUpdateRequest(
        endDate = TimeSource.tomorrow(),
        reasonCode = "OTHER",
      ),
    )

    allocationRepository.findById(1).get().also { it.endDate isEqualTo TimeSource.tomorrow() }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
    )

    assertThat(webTestClient.retrieveAdvanceAttendance(1).prisonerNumber).isEqualTo("A11111A")
  }

  @Sql("classpath:test_data/seed-activity-with-advance-attendances-2.sql")
  @Test
  fun `update allocation end date to before instance with advance attendance`() {
    assertThat(webTestClient.getScheduledInstancesByIds(3)!!.first().advanceAttendances).extracting("prisonerNumber").contains("C33333C")

    webTestClient.updateAllocation(
      MOORLAND_PRISON_CODE,
      4,
      AllocationUpdateRequest(
        endDate = LocalDate.now().plusDays(1),
        reasonCode = "OTHER",
      ),
    )

    allocationRepository.findById(4).get().also {
      it.endDate isEqualTo LocalDate.now().plusDays(1)
      it.prisonerStatus isEqualTo PrisonerStatus.ACTIVE
    }

    assertThat(webTestClient.getScheduledInstancesByIds(3)!!.first().advanceAttendances).isEmpty()
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `update allocation exclusions`() {
    webTestClient.updateAllocation(
      PENTONVILLE_PRISON_CODE,
      1,
      AllocationUpdateRequest(
        exclusions = listOf(
          Slot(
            weekNumber = 1,
            timeSlot = TimeSlot.AM,
            monday = true,
            daysOfWeek = setOf(DayOfWeek.MONDAY),
          ),
        ),
      ),
    )

    val allocation = webTestClient.getAllocationBy(1)!!

    with(allocation.exclusions) {
      this hasSize 1
      this.first().daysOfWeek isEqualTo setOf(DayOfWeek.MONDAY)
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
    )
  }

  @Sql("classpath:test_data/seed-activity-id-1.sql")
  @Test
  fun `suspend allocations - add planned suspension for the future`() {
    webTestClient.suspendAllocation(
      PENTONVILLE_PRISON_CODE,
      SuspendPrisonerRequest(
        prisonerNumber = "A11111A",
        allocationIds = listOf(1, 4),
        suspendFrom = 5.daysFromNow(),
        status = PrisonerStatus.SUSPENDED,
      ),
      "PVI",
    )

    listOf(1L, 4L).forEach {
      with(webTestClient.getAllocationBy(it)) {
        status isEqualTo PrisonerStatus.ACTIVE
        plannedSuspension!!.plannedStartDate isEqualTo 5.daysFromNow()
        plannedSuspension.plannedEndDate isEqualTo null
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 4),
    )
  }

  @Sql("classpath:test_data/seed-allocation-for-manual-suspension.sql")
  @Test
  fun `suspend allocation - add planned suspension to start immediately`() {
    webTestClient.suspendAllocation(
      PENTONVILLE_PRISON_CODE,
      SuspendPrisonerRequest(
        prisonerNumber = "A11111A",
        allocationIds = listOf(1, 2),
        suspendFrom = TimeSource.today(),
        status = PrisonerStatus.SUSPENDED,
      ),
      "PVI",
    )

    listOf(1L, 2L).forEach {
      with(webTestClient.getAllocationBy(it)) {
        status isEqualTo PrisonerStatus.SUSPENDED
        plannedSuspension!!.plannedStartDate isEqualTo TimeSource.today()
        plannedSuspension.plannedEndDate isEqualTo null
      }
    }

    listOf(1L, 3L).forEach {
      with(attendanceRepository.getReferenceById(it)) {
        // These attendances are un-changed because the session started before the suspension
        status(AttendanceStatus.WAITING) isBool true
        attendanceReason isEqualTo null
        issuePayment isEqualTo null
      }
    }

    listOf(2L, 4L).forEach {
      with(attendanceRepository.getReferenceById(it)) {
        // These attendances are suspended because the session starts after the suspension
        status(AttendanceStatus.COMPLETED) isBool true
        attendanceReason isNotEqualTo null
        attendanceReason!!.code isEqualTo AttendanceReasonEnum.SUSPENDED
        issuePayment isEqualTo false
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 2),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_AMENDED, 2),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_AMENDED, 4),
    )
  }

  @Sql("classpath:test_data/seed-allocation-for-manual-suspension.sql")
  @Test
  fun `suspend allocation - add planned suspension without pay to start immediately`() {
    webTestClient.suspendAllocation(
      PENTONVILLE_PRISON_CODE,
      SuspendPrisonerRequest(
        prisonerNumber = "A11111A",
        allocationIds = listOf(1, 2),
        suspendFrom = TimeSource.today(),
        status = PrisonerStatus.SUSPENDED,
      ),
      "PVI",
    )

    listOf(1L, 2L).forEach {
      with(webTestClient.getAllocationBy(it)) {
        status isEqualTo PrisonerStatus.SUSPENDED
        plannedSuspension!!.plannedStartDate isEqualTo TimeSource.today()
        plannedSuspension.plannedEndDate isEqualTo null
      }
    }

    listOf(1L, 3L).forEach {
      with(attendanceRepository.getReferenceById(it)) {
        // These attendances are un-changed because the session started before the suspension
        status(AttendanceStatus.WAITING) isBool true
        attendanceReason isEqualTo null
        issuePayment isEqualTo null
      }
    }

    listOf(2L, 4L).forEach {
      with(attendanceRepository.getReferenceById(it)) {
        // These attendances are suspended because the session starts after the suspension
        status(AttendanceStatus.COMPLETED) isBool true
        attendanceReason isNotEqualTo null
        attendanceReason!!.code isEqualTo AttendanceReasonEnum.SUSPENDED
        issuePayment isEqualTo false
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 2),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_AMENDED, 2),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_AMENDED, 4),
    )
  }

  @Sql("classpath:test_data/seed-allocation-for-manual-suspension.sql")
  @Test
  fun `suspend allocation - add planned suspension with pay to start immediately`() {
    webTestClient.suspendAllocation(
      PENTONVILLE_PRISON_CODE,
      SuspendPrisonerRequest(
        prisonerNumber = "A11111A",
        allocationIds = listOf(1, 2),
        suspendFrom = TimeSource.today(),
        status = PrisonerStatus.SUSPENDED_WITH_PAY,
      ),
      "PVI",
    )

    listOf(1L, 2L).forEach {
      with(webTestClient.getAllocationBy(it)) {
        status isEqualTo PrisonerStatus.SUSPENDED_WITH_PAY
        plannedSuspension!!.plannedStartDate isEqualTo TimeSource.today()
        plannedSuspension.plannedEndDate isEqualTo null
      }
    }

    listOf(1L, 3L).forEach {
      with(attendanceRepository.getReferenceById(it)) {
        // These attendances are un-changed because the session started before the suspension
        status(AttendanceStatus.WAITING) isBool true
        attendanceReason isEqualTo null
        issuePayment isEqualTo null
      }
    }

    listOf(2L, 4L).forEach {
      with(attendanceRepository.getReferenceById(it)) {
        // These attendances are suspended because the session starts after the suspension
        status(AttendanceStatus.COMPLETED) isBool true
        attendanceReason isNotEqualTo null
        attendanceReason!!.code isEqualTo AttendanceReasonEnum.SUSPENDED
        issuePayment isEqualTo true
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 2),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_AMENDED, 2),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_AMENDED, 4),
    )
  }

  @Sql("classpath:test_data/seed-allocation-with-active-suspension.sql")
  @Test
  fun `unsuspend allocation - update planned suspension to end immediately`() {
    webTestClient.unsuspendAllocation(
      PENTONVILLE_PRISON_CODE,
      UnsuspendPrisonerRequest(
        prisonerNumber = "A11111A",
        allocationIds = listOf(1, 2),
        suspendUntil = TimeSource.today(),
      ),
      "PVI",
    )

    listOf(1L, 2L).forEach {
      with(webTestClient.getAllocationBy(it)) {
        status isEqualTo PrisonerStatus.ACTIVE
        plannedSuspension isEqualTo null
      }
    }

    listOf(1L, 3L).forEach {
      with(attendanceRepository.getReferenceById(it)) {
        // These attendances are un-changed because the session started before the suspension ended
        status(AttendanceStatus.COMPLETED) isBool true
        attendanceReason isNotEqualTo null
        attendanceReason!!.code isEqualTo AttendanceReasonEnum.SUSPENDED
        issuePayment isEqualTo false
      }
    }

    listOf(2L, 4L).forEach {
      with(attendanceRepository.getReferenceById(it)) {
        // These attendances are reset because the session starts after the suspension ended
        status(AttendanceStatus.WAITING) isBool true
        attendanceReason isEqualTo null
        issuePayment isEqualTo null
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 2),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_AMENDED, 2),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_AMENDED, 4),
    )
  }

  @Sql("classpath:test_data/seed-allocation-with-active-paid-suspension.sql")
  @Test
  fun `unsuspend with pay allocation - update planned suspension to end immediately`() {
    webTestClient.unsuspendAllocation(
      PENTONVILLE_PRISON_CODE,
      UnsuspendPrisonerRequest(
        prisonerNumber = "A11111A",
        allocationIds = listOf(1, 2),
        suspendUntil = TimeSource.today(),
      ),
      "PVI",
    )

    listOf(1L, 2L).forEach {
      with(webTestClient.getAllocationBy(it)) {
        status isEqualTo PrisonerStatus.ACTIVE
        plannedSuspension isEqualTo null
      }
    }

    listOf(1L, 3L).forEach {
      with(attendanceRepository.getReferenceById(it)) {
        // These attendances are un-changed because the session started before the suspension ended
        status(AttendanceStatus.COMPLETED) isBool true
        attendanceReason isNotEqualTo null
        attendanceReason!!.code isEqualTo AttendanceReasonEnum.SUSPENDED
        issuePayment isEqualTo true
      }
    }

    listOf(2L, 4L).forEach {
      with(attendanceRepository.getReferenceById(it)) {
        // These attendances are reset because the session starts after the suspension ended
        status(AttendanceStatus.WAITING) isBool true
        attendanceReason isEqualTo null
        issuePayment isEqualTo null
      }
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 1),
      ExpectedOutboundEvent(PRISONER_ALLOCATION_AMENDED, 2),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_AMENDED, 2),
      ExpectedOutboundEvent(PRISONER_ATTENDANCE_AMENDED, 4),
    )
  }

  private fun WebTestClient.updateAllocation(
    prisonCode: String,
    allocationId: Long,
    request: AllocationUpdateRequest,
    caseloadId: String? = CASELOAD_ID,
  ) = patch()
    .uri("/allocations/$prisonCode/allocationId/$allocationId")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
    .header(CASELOAD_ID, caseloadId)
    .exchange()
    .expectStatus().isAccepted
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Allocation::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.suspendAllocation(
    prisonCode: String,
    request: SuspendPrisonerRequest,
    caseloadId: String? = CASELOAD_ID,
  ) = post()
    .uri("/allocations/$prisonCode/suspend")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
    .header(CASELOAD_ID, caseloadId)
    .exchange()
    .expectStatus().isAccepted

  private fun WebTestClient.unsuspendAllocation(
    prisonCode: String,
    request: UnsuspendPrisonerRequest,
    caseloadId: String? = CASELOAD_ID,
  ) = post()
    .uri("/allocations/$prisonCode/unsuspend")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
    .header(CASELOAD_ID, caseloadId)
    .exchange()
    .expectStatus().isAccepted

  private fun WebTestClient.waitingListApplication(
    prisonCode: String,
    application: WaitingListApplicationRequest,
    caseloadId: String? = CASELOAD_ID,
  ) = post()
    .uri("/allocations/$prisonCode/waiting-list-application")
    .bodyValue(application)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
    .header(CASELOAD_ID, caseloadId)
    .exchange()

  private fun WebTestClient.getAllocationBy(allocationId: Long) = get()
    .uri("/allocations/id/$allocationId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Allocation::class.java)
    .returnResult().responseBody
}
