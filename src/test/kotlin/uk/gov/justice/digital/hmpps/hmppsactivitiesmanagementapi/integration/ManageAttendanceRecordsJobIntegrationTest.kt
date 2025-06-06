package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@TestPropertySource(
  properties = [
    "feature.events.sns.enabled=true",
    "feature.event.activities.prisoner.attendance-created=true",
    "feature.event.activities.prisoner.attendance-amended=true",
    "feature.event.activities.prisoner.attendance-expired=true",
  ],
)
class ManageAttendanceRecordsJobIntegrationTest : IntegrationTestBase() {

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  @Autowired
  private lateinit var attendanceRepository: AttendanceRepository

  @Autowired
  private lateinit var activityRepository: ActivityRepository

  @Autowired
  private lateinit var activityScheduleRepository: ActivityScheduleRepository

  @Autowired
  private lateinit var scheduledInstanceRepository: ScheduledInstanceRepository

  @Autowired
  private lateinit var allocationRepository: AllocationRepository

  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Sql("classpath:test_data/seed-activity-id-4.sql")
  @Test
  fun `Four attendance records are created, 2 for Maths level 1 AM and 2 for Maths Level 1 PM and four create events are emitted`() {
    val allocatedPrisoners = listOf(listOf("A22222A", "A11111A"), listOf("A44444A", "A33333A"))
    allocatedPrisoners.forEach {
      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers = it,
        prisoners = it.map { prisonNumber ->
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = prisonNumber, prisonId = "PVI")
        },
      )
    }

    assertThat(attendanceRepository.count()).isZero
    val activity = activityRepository.findById(4).orElseThrow()
    val activitySchedules = activityScheduleRepository.getAllByActivity(activity)

    with(activity) {
      assertThat(description).isEqualTo("Maths Level 1")
    }

    assertThat(activitySchedules).hasSize(2)

    var allAttendances = attendanceRepository.findAll()
    assertThat(allAttendances).hasSize(0)

    with(activitySchedules.findByDescription("Maths AM")) {
      allocationRepository.findByActivitySchedule(this) hasSize 2
      assertThat(instances()).hasSize(1)
      scheduledInstanceRepository.findById(instances().first().scheduledInstanceId)
        .orElseThrow { EntityNotFoundException("ScheduledInstance id ${this.activityScheduleId} not found") }
    }

    with(activitySchedules.findByDescription("Maths PM")) {
      allocationRepository.findByActivitySchedule(this) hasSize 2
      assertThat(instances()).hasSize(1)
      scheduledInstanceRepository.findById(instances().first().scheduledInstanceId)
        .orElseThrow { EntityNotFoundException("ScheduledInstance id ${this.activityScheduleId} not found") }
    }

    waitForJobs({ webTestClient.manageAttendanceRecords() })

    val activityAfter = activityRepository.findById(4).orElseThrow()
    val activitySchedulesAfter = activityScheduleRepository.getAllByActivity(activityAfter)
    log.info("ActivitySchedulesAfter count = ${activitySchedulesAfter.size}")

    allAttendances = attendanceRepository.findAll()

    with(activitySchedulesAfter.findByDescription("Maths AM")) {
      val scheduledInstance = scheduledInstanceRepository.findById(instances().first().scheduledInstanceId)
        .orElseThrow { EntityNotFoundException("ScheduledInstance id ${instances().first().scheduledInstanceId} not found") }
      assertThat(allAttendances.filter { it.scheduledInstance.scheduledInstanceId == scheduledInstance.scheduledInstanceId }).hasSize(2)
    }

    with(activitySchedulesAfter.findByDescription("Maths PM")) {
      val scheduledInstance = scheduledInstanceRepository.findById(instances().first().scheduledInstanceId)
        .orElseThrow { EntityNotFoundException("ScheduledInstance id ${instances().first().scheduledInstanceId} not found") }
      assertThat(allAttendances.filter { it.scheduledInstance.scheduledInstanceId == scheduledInstance.scheduledInstanceId }).hasSize(2)
    }

    assertThat(attendanceRepository.count()).isEqualTo(4)

    verify(eventsPublisher, times(4)).send(eventCaptor.capture())

    eventCaptor.allValues.map {
      assertThat(it.eventType).isEqualTo("activities.prisoner.attendance-created")
      assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
      assertThat(it.description).isEqualTo("A prisoner attendance has been created in the activities management service")
    }
  }

  @Sql("classpath:test_data/seed-activity-for-attendance-job.sql")
  @Test
  fun `Attendance records are not created for where there are exclusions`() {
    val allocatedPrisoners = listOf(listOf("G4793VF", "H4793VF"), listOf("A5193DY"))
    allocatedPrisoners.forEach {
      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers = it,
        prisoners = it.map { prisonNumber ->
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = prisonNumber, prisonId = "MDI")
        },
      )
    }

    assertThat(attendanceRepository.count()).isZero
    val activity = activityRepository.findById(1).orElseThrow()
    val activitySchedules = activityScheduleRepository.getAllByActivity(activity)

    assertThat(activity.description).isEqualTo("Maths Level 1")
    assertThat(activitySchedules).hasSize(1)

    with(activitySchedules.first()) {
      allocationRepository.findByActivitySchedule(this) hasSize 4
      instances() hasSize 2
      val scheduledInstances = scheduledInstanceRepository.findAll()
      assertThat(scheduledInstances).isNotEmpty
      assertThat(attendanceRepository.findAll()).hasSize(0)
    }

    waitForJobs({ webTestClient.manageAttendanceRecords() })

    val activityAfter = activityRepository.findById(1).orElseThrow()
    val activitySchedulesAfter = activityScheduleRepository.getAllByActivity(activityAfter)
    log.info("ActivitySchedulesAfter count = ${activitySchedulesAfter.size}")

    val allAttendances = attendanceRepository.findAll()
    assertThat(allAttendances).hasSize(6)
    assertThat(attendanceRepository.findAll().filter { it -> it.scheduledInstance.scheduledInstanceId == 1L }).hasSize(2)

    assertThat(attendanceRepository.findAll().filter { it -> it.scheduledInstance.scheduledInstanceId == 2L }).hasSize(4)

    verify(eventsPublisher, times(6)).send(eventCaptor.capture())

    eventCaptor.allValues.forEach {
      it.eventType isEqualTo "activities.prisoner.attendance-created"
      it.occurredAt isCloseTo LocalDateTime.now()
      it.description isEqualTo "A prisoner attendance has been created in the activities management service"
    }
  }

  @Sql("classpath:test_data/seed-activity-id-4.sql")
  @Test
  fun `Multiple calls on same day does not result in duplicate attendances`() {
    val allocatedPrisoners = listOf(listOf("A22222A", "A11111A"), listOf("A44444A", "A33333A"))
    allocatedPrisoners.forEach {
      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers = it,
        prisoners = it.map { prisonNumber ->
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = prisonNumber, prisonId = "PVI")
        },
      )
    }

    assertThat(attendanceRepository.count()).isZero

    waitForJobs({ webTestClient.manageAttendanceRecords() })

    waitForJobs({ webTestClient.manageAttendanceRecords() }, 2)

    assertThat(attendanceRepository.count()).isEqualTo(4)
  }

  @Sql("classpath:test_data/seed-activity-id-5.sql")
  @Test
  fun `Attendance records should be created for gym induction AM when attendance is not required on the activity`() {
    assertThat(attendanceRepository.count()).isZero
    val activity = activityRepository.findById(5).orElseThrow()
    val activitySchedules = activityScheduleRepository.getAllByActivity(activity)

    with(activity) {
      assertThat(description).isEqualTo("Gym induction")
    }

    assertThat(activitySchedules).hasSize(1)
    assertThat(attendanceRepository.findAll()).hasSize(0)

    with(activitySchedules.findByDescription("Gym induction AM")) {
      allocationRepository.findByActivitySchedule(this) hasSize 2
      assertThat(instances()).hasSize(1)
    }

    waitForJobs({ webTestClient.manageAttendanceRecords() })

    val activityAfter = activityRepository.findById(5).orElseThrow()
    val activitySchedulesAfter = activityScheduleRepository.getAllByActivity(activityAfter)

    val scheduledInstanceId = activitySchedulesAfter.findByDescription("Gym induction AM").instances().first().scheduledInstanceId
    assertThat(attendanceRepository.findAll().filter { it.scheduledInstance.scheduledInstanceId == scheduledInstanceId }).isNotEmpty

    assertThat(attendanceRepository.count()).isEqualTo(2)
  }

  @Sql("classpath:test_data/seed-activity-with-advance-attendances-1.sql")
  @Test
  fun `Attendance records should be created for including any advance attendances for not required prisoners`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      prisonerNumbers = listOf("A11111A"),
      prisoners = listOf(PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A11111A", prisonId = "PVI")),
    )

    assertThat(attendanceRepository.count()).isZero

    val activity = activityRepository.findById(1).orElseThrow()
    val activitySchedules = activityScheduleRepository.getAllByActivity(activity)

    with(activity) {
      assertThat(description).isEqualTo("Gym induction")
    }

    assertThat(activitySchedules).hasSize(1)
    assertThat(attendanceRepository.findAll()).hasSize(0)

    with(activitySchedules.findByDescription("Gym induction AM")) {
      allocationRepository.findByActivitySchedule(this) hasSize 1
      assertThat(instances()).hasSize(1)
    }

    waitForJobs({ webTestClient.manageAttendanceRecords() })

    val attendances = attendanceRepository.findAll()

    assertThat(attendances).hasSize(1)

    val attendance = webTestClient.getAttendanceById(attendances.first().attendanceId)!!

    with(attendance) {
      assertThat(prisonerNumber).isEqualTo("A11111A")
      assertThat(status).isEqualTo(AttendanceStatus.COMPLETED.toString())
      assertThat(recordedBy).isEqualTo("Activities Management Service")
      assertThat(recordedTime).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))

      val attendanceHistory = attendanceHistory!!
      assertThat(attendanceHistory).hasSize(3)
      with(attendanceHistory[0]) {
        assertThat(attendanceReason?.id).isEqualTo(3)
        assertThat(recordedBy).isEqualTo("FRED SMITH")
        assertThat(recordedTime).isEqualTo(LocalDateTime.parse("2022-10-15T10:00"))
        assertThat(issuePayment).isTrue
        assertThat(comment).isNull()
        assertThat(incentiveLevelWarningIssued).isNull()
        assertThat(otherAbsenceReason).isNull()
        assertThat(caseNoteText).isNull()
      }
      with(attendanceHistory[1]) {
        assertThat(attendanceReason?.id).isEqualTo(3)
        assertThat(recordedBy).isEqualTo("ALICE JONES")
        assertThat(recordedTime).isEqualTo(LocalDateTime.parse("2022-10-14T11:00"))
        assertThat(issuePayment).isFalse
        assertThat(comment).isNull()
        assertThat(incentiveLevelWarningIssued).isNull()
        assertThat(otherAbsenceReason).isNull()
        assertThat(caseNoteText).isNull()
      }
      with(attendanceHistory[2]) {
        assertThat(attendanceReason?.id).isEqualTo(3)
        assertThat(recordedBy).isEqualTo("SARAH WILLIAMS")
        assertThat(recordedTime).isEqualTo(LocalDateTime.parse("2022-10-13T11:00"))
        assertThat(issuePayment).isTrue
        assertThat(comment).isNull()
        assertThat(incentiveLevelWarningIssued).isNull()
        assertThat(otherAbsenceReason).isNull()
        assertThat(caseNoteText).isNull()
      }
    }

    webTestClient.get()
      .uri("/advance-attendances/1")
      .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
      .exchange()
      .expectStatus().isNotFound
  }

  @Sql("classpath:test_data/seed-attendances-yesterdays-completed.sql")
  @Test
  fun `Yesterdays completed attendance records remain in status COMPLETED and no sync events are emitted`() {
    val yesterday = LocalDate.now().minusDays(1)

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
      assertThat(it.scheduledInstance.sessionDate).isEqualTo(yesterday)
    }

    waitForJobs({ webTestClient.manageAttendanceRecordsWithExpiry() }, 2)

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
    }

    verifyNoInteractions(eventsPublisher)
  }

  @Sql("classpath:test_data/seed-attendances-yesterdays-waiting.sql")
  @Test
  fun `Yesterdays waiting attendance records emit an expired sync event and remain in WAITING status`() {
    val yesterday = LocalDate.now().minusDays(1)

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(it.scheduledInstance.sessionDate).isEqualTo(yesterday)
    }

    waitForJobs({ webTestClient.manageAttendanceRecordsWithExpiry() }, 2)

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    eventCaptor.allValues.map {
      assertThat(it.eventType).isEqualTo("activities.prisoner.attendance-expired")
      assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
      assertThat(it.description).isEqualTo("An unmarked prisoner attendance has been expired in the activities management service")
    }
  }

  @Sql("classpath:test_data/seed-attendances-two-days-old-waiting.sql")
  @Test
  fun `Two day old waiting attendance records do not emit an expired sync event`() {
    val twoDaysAgo = LocalDate.now().minusDays(2)

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
      assertThat(it.scheduledInstance.sessionDate).isEqualTo(twoDaysAgo)
    }

    waitForJobs({ webTestClient.manageAttendanceRecordsWithExpiry() }, 2)

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.WAITING)
    }

    verifyNoInteractions(eventsPublisher)
  }

  @Sql("classpath:test_data/seed-attendances-for-today.sql")
  @Test
  fun `Attendance records for today will not emit an expired sync event - too early`() {
    val today = LocalDate.now()

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isIn(AttendanceStatus.WAITING, AttendanceStatus.COMPLETED)
      assertThat(it.scheduledInstance.sessionDate).isEqualTo(today)
    }

    waitForJobs({ webTestClient.manageAttendanceRecordsWithExpiry() }, 2)

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isIn(AttendanceStatus.WAITING, AttendanceStatus.COMPLETED)
    }

    verifyNoInteractions(eventsPublisher)
  }

  @Sql("classpath:test_data/seed-activity-with-previous-current-future-deallocation.sql")
  @Test
  fun `Two attendance records are created when there is a previously ended, a current and a future deallocations present`() {
    val allocatedPrisoners = listOf(listOf("A22222A", "A11111A"))
    allocatedPrisoners.forEach {
      prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
        prisonerNumbers = it,
        prisoners = it.map { prisonNumber ->
          PrisonerSearchPrisonerFixture.instance(prisonerNumber = prisonNumber, prisonId = "PVI")
        },
      )
    }

    assertThat(attendanceRepository.count()).isZero
    val activity = activityRepository.findById(1).orElseThrow()
    val activitySchedules = activityScheduleRepository.getAllByActivity(activity)

    with(activity) {
      assertThat(description).isEqualTo("Basic retirement")
    }

    assertThat(activitySchedules).hasSize(1)
    assertThat(attendanceRepository.findAll()).hasSize(0)

    with(activitySchedules.first()) {
      allocationRepository.findByActivitySchedule(this) hasSize 3
      assertThat(instances()).hasSize(1)
      scheduledInstanceRepository.findById(instances().first().scheduledInstanceId)
        .orElseThrow { EntityNotFoundException("ScheduledInstance id ${this.activityScheduleId} not found") }
    }

    waitForJobs({ webTestClient.manageAttendanceRecords() })

    val attendanceRecords = attendanceRepository.findAll()
    assertThat(attendanceRecords).hasSize(2)

    with(attendanceRecords) {
      this.single { it.prisonerNumber == "A11111A" && it.status() == AttendanceStatus.WAITING }
      this.single { it.prisonerNumber == "A22222A" && it.status() == AttendanceStatus.WAITING }
    }

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    eventCaptor.allValues.map {
      assertThat(it.eventType).isEqualTo("activities.prisoner.attendance-created")
      assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
      assertThat(it.description).isEqualTo("A prisoner attendance has been created in the activities management service")
    }
  }

  private fun List<ActivitySchedule>.findByDescription(description: String) = first { it.description.uppercase() == description.uppercase() }

  private fun WebTestClient.manageAttendanceRecords() {
    post()
      .uri("/job/manage-attendance-records")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated
  }

  private fun WebTestClient.manageAttendanceRecordsWithExpiry() {
    post()
      .uri("/job/manage-attendance-records?withExpiry=true")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated
  }

  private fun WebTestClient.getAttendanceById(id: Long) = get()
    .uri("/attendances/$id")
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Attendance::class.java)
    .returnResult().responseBody
}
