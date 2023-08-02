package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.Status
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
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
  private val prisonApiClient: PrisonApiClient = mock()
  private val service = WaitingListService(scheduleRepository, prisonApiClient)

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

    val request = WaitingListApplicationRequest(
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

    val request = WaitingListApplicationRequest(
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
  fun `add prisoner to waiting list succeeds`() {
    prisonApiClient.stub {
      on { getPrisonerDetails(prisonerNumber = "123456") } doReturn Mono.just(
        InmateDetailFixture.instance(
          offenderNo = "123456",
          status = "ACTIVE IN",
          agencyId = DEFAULT_CASELOAD_ID,
          bookingId = 1L,
        ),
      )

      val request = WaitingListApplicationRequest(
        prisonerNumber = "123456",
        activityScheduleId = 1L,
        applicationDate = LocalDate.now(),
        requestedBy = "Bob",
        comments = "Bob's comments",
        status = Status.PENDING,
      )

      service.addPrisoner(DEFAULT_CASELOAD_ID, request, "Test user")
      verify(scheduleRepository).saveAndFlush(schedule)
    }
  }

  @Test
  fun `can add multiple waiting list records for the same prisoner`() {
    prisonApiClient.stub {
      on { getPrisonerDetails(prisonerNumber = "123456") } doReturn Mono.just(
        InmateDetailFixture.instance(
          offenderNo = "123456",
          status = "ACTIVE IN",
          agencyId = DEFAULT_CASELOAD_ID,
          bookingId = 1L,
        ),
      )

      val request = WaitingListApplicationRequest(
        prisonerNumber = "123456",
        activityScheduleId = 1L,
        applicationDate = LocalDate.now(),
        requestedBy = "Bob",
        comments = "Bob's comments",
        status = Status.DECLINED,
      )

      service.addPrisoner(DEFAULT_CASELOAD_ID, request, "Test user")
      service.addPrisoner(DEFAULT_CASELOAD_ID, request.copy(status = Status.PENDING), "Test user")
      verify(scheduleRepository, times(2)).saveAndFlush(schedule)
    }
  }
}
