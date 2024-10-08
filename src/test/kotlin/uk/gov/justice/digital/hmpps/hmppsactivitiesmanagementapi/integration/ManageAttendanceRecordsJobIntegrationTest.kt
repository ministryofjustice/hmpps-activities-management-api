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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AttendanceRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ScheduledInstanceRepository
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

    with(activitySchedules.findByDescription("Maths AM")) {
      assertThat(allocations()).hasSize(2)
      assertThat(instances()).hasSize(1)
      val scheduledInstance = scheduledInstanceRepository.findById(instances().first().scheduledInstanceId)
        .orElseThrow { EntityNotFoundException("ScheduledInstance id ${this.activityScheduleId} not found") }
      assertThat(scheduledInstance.attendances).isEmpty()
    }

    with(activitySchedules.findByDescription("Maths PM")) {
      assertThat(allocations()).hasSize(2)
      assertThat(instances()).hasSize(1)
      val scheduledInstance = scheduledInstanceRepository.findById(instances().first().scheduledInstanceId)
        .orElseThrow { EntityNotFoundException("ScheduledInstance id ${this.activityScheduleId} not found") }
      assertThat(scheduledInstance.attendances).isEmpty()
    }

    webTestClient.manageAttendanceRecords()

    val activityAfter = activityRepository.findById(4).orElseThrow()
    val activitySchedulesAfter = activityScheduleRepository.getAllByActivity(activityAfter)
    log.info("ActivitySchedulesAfter count = ${activitySchedulesAfter.size}")

    with(activitySchedulesAfter.findByDescription("Maths AM")) {
      val scheduledInstance = scheduledInstanceRepository.findById(instances().first().scheduledInstanceId)
        .orElseThrow { EntityNotFoundException("ScheduledInstance id ${instances().first().scheduledInstanceId} not found") }
      log.info("ScheduledInstanceId (Maths AM) = ${scheduledInstance.scheduledInstanceId} attendances ${scheduledInstance.attendances.size}")
      assertThat(scheduledInstance.attendances).hasSize(2)
    }

    with(activitySchedulesAfter.findByDescription("Maths PM")) {
      val scheduledInstance = scheduledInstanceRepository.findById(instances().first().scheduledInstanceId)
        .orElseThrow { EntityNotFoundException("ScheduledInstance id ${instances().first().scheduledInstanceId} not found") }
      log.info("ScheduledInstanceId (Maths PM) = ${scheduledInstance.scheduledInstanceId} attendances ${scheduledInstance.attendances.size}")
      assertThat(scheduledInstance.attendances).hasSize(2)
    }

    assertThat(attendanceRepository.count()).isEqualTo(4)

    verify(eventsPublisher, times(4)).send(eventCaptor.capture())

    eventCaptor.allValues.map {
      assertThat(it.eventType).isEqualTo("activities.prisoner.attendance-created")
      assertThat(it.occurredAt).isCloseTo(LocalDateTime.now(), Assertions.within(60, ChronoUnit.SECONDS))
      assertThat(it.description).isEqualTo("A prisoner attendance has been created in the activities management service")
    }
  }

  @Sql("classpath:test_data/seed-activity-with-active-exclusions.sql")
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
      allocations() hasSize 4
      instances() hasSize 2
      val scheduledInstances = scheduledInstanceRepository.findAll()
      assertThat(scheduledInstances).isNotEmpty
      scheduledInstances.forEach { assertThat(it.attendances).isEmpty() }
    }

    webTestClient.manageAttendanceRecords()

    val activityAfter = activityRepository.findById(1).orElseThrow()
    val activitySchedulesAfter = activityScheduleRepository.getAllByActivity(activityAfter)
    log.info("ActivitySchedulesAfter count = ${activitySchedulesAfter.size}")

    val morningSession = scheduledInstanceRepository.getReferenceById(1)
    log.info("ScheduledInstanceId (AM) = ${morningSession.scheduledInstanceId} attendances ${morningSession.attendances.size}")
    assertThat(morningSession.attendances).hasSize(2)

    val afternoonSession = scheduledInstanceRepository.getReferenceById(2)
    log.info("ScheduledInstanceId (PM) = ${afternoonSession.scheduledInstanceId} attendances ${afternoonSession.attendances.size}")
    assertThat(afternoonSession.attendances).hasSize(4)

    assertThat(attendanceRepository.count()).isEqualTo(6)

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

    webTestClient.manageAttendanceRecords()
    webTestClient.manageAttendanceRecords()

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

    with(activitySchedules.findByDescription("Gym induction AM")) {
      assertThat(allocations()).hasSize(2)
      assertThat(instances()).hasSize(1)
      assertThat(instances().first().attendances).isEmpty()
    }

    webTestClient.manageAttendanceRecords()

    val activityAfter = activityRepository.findById(5).orElseThrow()
    val activitySchedulesAfter = activityScheduleRepository.getAllByActivity(activityAfter)

    with(activitySchedulesAfter.findByDescription("Gym induction AM")) {
      assertThat(instances().first().attendances).isNotEmpty()
    }

    assertThat(attendanceRepository.count()).isEqualTo(2)
  }

  @Sql("classpath:test_data/seed-attendances-yesterdays-completed.sql")
  @Test
  fun `Yesterdays completed attendance records remain in status COMPLETED and no sync events are emitted`() {
    val yesterday = LocalDate.now().minusDays(1)

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isEqualTo(AttendanceStatus.COMPLETED)
      assertThat(it.scheduledInstance.sessionDate).isEqualTo(yesterday)
    }

    webTestClient.manageAttendanceRecordsWithExpiry()

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

    webTestClient.manageAttendanceRecordsWithExpiry()

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

    webTestClient.manageAttendanceRecordsWithExpiry()

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

    webTestClient.manageAttendanceRecordsWithExpiry()

    attendanceRepository.findAll().forEach {
      assertThat(it.status()).isIn(AttendanceStatus.WAITING, AttendanceStatus.COMPLETED)
    }

    verifyNoInteractions(eventsPublisher)
  }

  private fun List<ActivitySchedule>.findByDescription(description: String) =
    first { it.description.uppercase() == description.uppercase() }

  private fun WebTestClient.manageAttendanceRecords() {
    post()
      .uri("/job/manage-attendance-records")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated
    Thread.sleep(1000)
  }

  private fun WebTestClient.manageAttendanceRecordsWithExpiry() {
    post()
      .uri("/job/manage-attendance-records?withExpiry=true")
      .accept(MediaType.TEXT_PLAIN)
      .exchange()
      .expectStatus().isCreated
    Thread.sleep(1000)
  }
}
