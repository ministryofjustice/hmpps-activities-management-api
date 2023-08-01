package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.Status
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.DEFAULT_CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeCaseLoad
import java.time.LocalDate

@ExtendWith(FakeCaseLoad::class)
class WaitingListServiceTest {

  private val activity = activityEntity()
  private val schedule = activity.schedules().first()
  private val scheduleRepository: ActivityScheduleRepository =
    mock { on { findBy(schedule.activityScheduleId, DEFAULT_CASELOAD_ID) } doReturn schedule }
  private val waitingListRepository: WaitingListRepository = mock {}
  private val prisonApiClient: PrisonApiClient = mock()
  private val service = WaitingListService(scheduleRepository, waitingListRepository, prisonApiClient)

  @Test
  fun `fails if does not have case load access`() {
    assertThatThrownBy {
      service.addPrisoner("XYZ", mock(), "test")
    }.isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `fails if prisoner is at different prison to the activities prison`() {
    prisonApiClient.stub {
      on { getPrisonerDetails(prisonerNumber = "123456") } doReturn Mono.just(
        InmateDetailFixture.instance(
          offenderNo = "123456",
          status = "INACTIVE OUT",
          agencyId = DEFAULT_CASELOAD_ID,
        ),
      )
    }

    val request = WaitingListCreateRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = Status.PENDING,
    )

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_ID, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner 123456 is not active in/out at prison $DEFAULT_CASELOAD_ID")
  }

  @Test
  fun `fails if prisoner has no booking id`() {
    prisonApiClient.stub {
      on { getPrisonerDetails(prisonerNumber = "123456") } doReturn Mono.just(
        InmateDetailFixture.instance(
          offenderNo = "123456",
          status = "ACTIVE IN",
          agencyId = DEFAULT_CASELOAD_ID,
          bookingId = null,
        ),
      )
    }

    val request = WaitingListCreateRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = Status.PENDING,
    )

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_ID, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner 123456 has no booking id at prison $DEFAULT_CASELOAD_ID")
  }

  @Test
  fun `fails if activity category is 'not in work'`() {
    val request = WaitingListCreateRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = Status.PENDING,
    )

    activity.activityCategory = activityCategory(code = "SAA_NOT_IN_WORK")

    assertThatThrownBy {
      service.addPrisoner(DEFAULT_CASELOAD_ID, request, "test")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add prisoner 123456 to the waiting list for a 'not in work' activity")
  }

  @Test
  fun `fails if activity has ended or ends today`() {
    activity.endDate = LocalDate.now()

    val request = WaitingListCreateRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = Status.PENDING,
    )

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_ID, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Activity must end in the future to add a prisoner to a waiting list")
  }

  @Test
  fun `fails if application date is in the future`() {
    val request = WaitingListCreateRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.tomorrow(),
      requestedBy = "Test",
      status = Status.PENDING,
    )

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_ID, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Application date cannot be not be in the future")
  }

  @Test
  fun `fails if pending application already exists for prisoner`() {
    val request = WaitingListCreateRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = Status.PENDING,
    )

    mock<WaitingList> { on { status } doReturn WaitingListStatus.PENDING }.also { pending ->
      waitingListRepository.stub {
        on {
          findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
            DEFAULT_CASELOAD_ID,
            "123456",
            schedule,
          )
        } doReturn listOf(pending)
      }
    }

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_ID, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("A pending waiting list application already exists for 123456")
  }

  @Test
  fun `fails if approved application already exists for prisoner`() {
    val request = WaitingListCreateRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = Status.PENDING,
    )

    mock<WaitingList> { on { status } doReturn WaitingListStatus.APPROVED }.also { approved ->
      waitingListRepository.stub {
        on {
          findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
            DEFAULT_CASELOAD_ID,
            "123456",
            schedule,
          )
        } doReturn listOf(approved)
      }
    }

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_ID, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("An approved waiting list application already exists for 123456")
  }

  @Test
  fun `fails if prisoner already allocated (in future)`() {
    val request = WaitingListCreateRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = Status.PENDING,
    )

    schedule.allocatePrisoner(
      prisonerNumber = "123456".toPrisonerNumber(),
      payBand = mock(),
      bookingId = 1L,
      startDate = TimeSource.tomorrow(),
      allocatedBy = "Test",
    )

    waitingListRepository.stub {
      on {
        findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
          DEFAULT_CASELOAD_ID,
          "123456",
          schedule,
        )
      } doReturn emptyList()
    }

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_ID, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner 123456 cannot be added to the waiting list because they are already allocated")
  }
}
