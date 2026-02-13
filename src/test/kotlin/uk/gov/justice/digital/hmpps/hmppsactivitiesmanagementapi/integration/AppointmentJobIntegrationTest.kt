package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.NonResidentialUsageDto.UsageType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nomismapping.api.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.MovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.daysAgo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.RISLEY_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AppointmentCancelledOnTransferEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent.APPOINTMENT_INSTANCE_DELETED
import java.time.LocalDate
import java.util.*

@Deprecated("Remove when manage appointment attendees job always uses SQS")
@TestPropertySource(
  properties = [
    "feature.jobs.sqs.activate.allocations.enabled=false",
    "feature.jobs.sqs.manage.attendances.enabled=false",
  ],
)
class AppointmentJobIntegrationTest : AppointmentsIntegrationTestBase() {

  @MockitoBean
  private lateinit var auditService: AuditService

  private val prisonNumber = "A1234BC"

  private val activeInPrisoner = PrisonerSearchPrisonerFixture.instance(
    prisonerNumber = "B2345CD",
    inOutStatus = Prisoner.InOutStatus.IN,
    status = "ACTIVE IN",
    prisonId = RISLEY_PRISON_CODE,
    lastMovementType = null,
    confirmedReleaseDate = null,
  )

  private val activeInDifferentPrison = PrisonerSearchPrisonerFixture.instance(
    prisonerNumber = prisonNumber,
    inOutStatus = Prisoner.InOutStatus.IN,
    status = "ACTIVE IN",
    prisonId = MOORLAND_PRISON_CODE,
    lastMovementType = null,
    confirmedReleaseDate = null,
  )

  private val prisonerReleasedToday = PrisonerSearchPrisonerFixture.instance(
    prisonerNumber = prisonNumber,
    inOutStatus = Prisoner.InOutStatus.OUT,
    status = "INACTIVE OUT",
    prisonId = null,
    lastMovementType = MovementType.RELEASE,
    confirmedReleaseDate = LocalDate.now(),
  )

  private val expiredMovement = movement(prisonerNumber = prisonNumber, fromPrisonCode = RISLEY_PRISON_CODE, movementDate = 21.daysAgo())
  private val nonExpiredMovement = movement(prisonerNumber = prisonNumber, fromPrisonCode = RISLEY_PRISON_CODE, movementDate = 4.daysAgo())

  @BeforeEach
  fun setUp() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers("B2345CD", "C3456DE")

    val dpsLocation = dpsLocation(UUID.fromString("11111111-1111-1111-1111-111111111111"), "RSI")

    locationsInsidePrisonApiMockServer.stubLocationsForUsageType(
      prisonCode = "RSI",
      usageType = UsageType.APPOINTMENT,
      locations = listOf(dpsLocation),
    )

    nomisMappingApiMockServer.stubMappingsFromDpsIds(
      listOf(
        NomisDpsLocationMapping(dpsLocation.id, 123),
      ),
    )
  }

  @Sql("classpath:test_data/seed-manage-appointments-job.sql")
  @Test
  fun `do not remove released prisoner from future appointments when days from now does not include their appointments`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf("B2345CD"), listOf(activeInPrisoner))

    waitForJobs({ webTestClient.manageAppointmentAttendees(0) })

    with(webTestClient.getAppointmentSeriesById(1)!!.appointments.filterNot { it.isDeleted }) {
      flatMap { it.attendees } hasSize 10
    }

    with(webTestClient.getAppointmentSetDetailsById(1)!!.appointments.filterNot { it.isDeleted }) {
      flatMap { it.attendees } hasSize 3
    }

    validateNoMessagesSent()
    verifyNoInteractions(auditService)
  }

  @Sql("classpath:test_data/seed-manage-appointments-job.sql")
  @Test
  fun `remove released prisoner from future appointments`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf(prisonNumber, "B2345CD"), listOf(prisonerReleasedToday, activeInPrisoner))

    waitForJobs({ webTestClient.manageAppointmentAttendees(1) })

    with(webTestClient.getAppointmentSeriesById(1)!!.appointments.filterNot { it.isDeleted }) {
      flatMap { it.attendees } hasSize 7
      single { it.id == 1L }.attendees.map { it.prisonerNumber } containsExactlyInAnyOrder listOf(
        prisonNumber,
        "B2345CD",
      )
      filterNot { it.id == 1L }.flatMap { it.attendees }.map { it.prisonerNumber }.toSet() isEqualTo setOf("B2345CD")
    }

    with(webTestClient.getAppointmentSetDetailsById(1)!!.appointments.filterNot { it.isDeleted }) {
      flatMap { it.attendees } hasSize 2
      flatMap { it.attendees }.map { it.prisoner.prisonerNumber }.toSet() isEqualTo setOf("B2345CD", "C3456DE")
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 20, "OIC"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 6, "CHAP"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 10, "CHAP"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 4, "CHAP"),
    )

    verify(auditService, times(4)).logEvent(any<AppointmentCancelledOnTransferEvent>())
  }

  @Sql("classpath:test_data/seed-manage-appointments-job.sql")
  @Test
  fun `do not remove transferred prisoner from future appointments when transfer was less than expired days ago`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf(prisonNumber, "B2345CD"), listOf(activeInDifferentPrison, activeInPrisoner))
    prisonApiMockServer.stubPrisonerMovements(listOf(prisonNumber), listOf(nonExpiredMovement))

    waitForJobs({ webTestClient.manageAppointmentAttendees(1) })

    with(webTestClient.getAppointmentSeriesById(1)!!.appointments.filterNot { it.isDeleted }) {
      flatMap { it.attendees } hasSize 10
    }

    with(webTestClient.getAppointmentSetDetailsById(1)!!.appointments.filterNot { it.isDeleted }) {
      flatMap { it.attendees } hasSize 3
    }

    validateNoMessagesSent()
    verifyNoInteractions(auditService)
  }

  @Sql("classpath:test_data/seed-manage-appointments-job.sql")
  @Test
  fun `remove transferred prisoner from future appointments`() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(listOf(prisonNumber, "B2345CD"), listOf(activeInDifferentPrison, activeInPrisoner))
    prisonApiMockServer.stubPrisonerMovements(listOf(prisonNumber), listOf(expiredMovement))

    waitForJobs({ webTestClient.manageAppointmentAttendees(1) })

    with(webTestClient.getAppointmentSeriesById(1)!!.appointments.filterNot { it.isDeleted }) {
      flatMap { it.attendees } hasSize 7
      single { it.id == 1L }.attendees.map { it.prisonerNumber }.toSet() isEqualTo setOf(prisonNumber, "B2345CD")
      filterNot { it.id == 1L }.flatMap { it.attendees }.map { it.prisonerNumber }.toSet() isEqualTo setOf("B2345CD")
    }

    with(webTestClient.getAppointmentSetDetailsById(1)!!.appointments.filterNot { it.isDeleted }) {
      flatMap { it.attendees } hasSize 2
      flatMap { it.attendees }.map { it.prisoner.prisonerNumber }.toSet() isEqualTo setOf("B2345CD", "C3456DE")
    }

    validateOutboundEvents(
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 10, "CHAP"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 4, "CHAP"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 6, "CHAP"),
      ExpectedOutboundEvent(APPOINTMENT_INSTANCE_DELETED, 20, "OIC"),
    )

    verify(auditService, times(4)).logEvent(any<AppointmentCancelledOnTransferEvent>())
  }
}
