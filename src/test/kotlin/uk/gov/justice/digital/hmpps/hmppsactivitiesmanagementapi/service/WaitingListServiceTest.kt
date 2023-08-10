package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.WaitingListApplicationUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.CaseloadAccessException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.DEFAULT_CASELOAD_PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeCaseLoad
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.toModel
import java.time.LocalDate
import java.util.Optional

@ExtendWith(FakeCaseLoad::class)
class WaitingListServiceTest {

  private val activity = activityEntity()
  private val schedule = activity.schedules().first()
  private val scheduleRepository: ActivityScheduleRepository =
    mock { on { findBy(schedule.activityScheduleId, DEFAULT_CASELOAD_PENTONVILLE) } doReturn schedule }
  private val waitingListRepository: WaitingListRepository = mock {}
  private val prisonApiClient: PrisonApiClient = mock()
  private val service = WaitingListService(scheduleRepository, waitingListRepository, prisonApiClient)
  private val waitingListCaptor = argumentCaptor<WaitingList>()

  @Test
  fun `add prisoner fails if does not have case load access`() {
    assertThatThrownBy {
      service.addPrisoner("WRONG_CASELOAD", mock(), "test")
    }.isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `fails if prisoner is at different prison to the activities prison`() {
    prisonApiClient.stub {
      on { getPrisonerDetails(prisonerNumber = "123456") } doReturn Mono.just(
        InmateDetailFixture.instance(
          offenderNo = "123456",
          status = "INACTIVE OUT",
          agencyId = DEFAULT_CASELOAD_PENTONVILLE,
        ),
      )
    }

    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Tim Harrison is not resident at this prison")
  }

  @Test
  fun `fails if status not PENDING, APPROVED or DECLINED`() {
    prisonApiClient.stub {
      on { getPrisonerDetails(prisonerNumber = "123456") } doReturn Mono.just(
        InmateDetailFixture.instance(
          offenderNo = "123456",
          status = "ACTIVE IN",
          agencyId = DEFAULT_CASELOAD_PENTONVILLE,
          bookingId = 1L,
        ),
      )
    }

    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = 1L,
      applicationDate = TimeSource.today(),
      requestedBy = "Bob",
      comments = "Bob's comments",
      status = WaitingListStatus.PENDING,
    )

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request.copy(status = WaitingListStatus.ALLOCATED), "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Only statuses of PENDING, APPROVED and DECLINED are allowed when adding a prisoner to a waiting list")

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request.copy(status = WaitingListStatus.REMOVED), "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Only statuses of PENDING, APPROVED and DECLINED are allowed when adding a prisoner to a waiting list")
  }

  @Test
  fun `fails if prisoner has no booking id`() {
    prisonApiClient.stub {
      on { getPrisonerDetails(prisonerNumber = "123456") } doReturn Mono.just(
        InmateDetailFixture.instance(
          offenderNo = "123456",
          status = "ACTIVE IN",
          agencyId = DEFAULT_CASELOAD_PENTONVILLE,
          bookingId = null,
        ),
      )
    }

    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Prisoner 123456 has no booking id at prison $DEFAULT_CASELOAD_PENTONVILLE")
  }

  @Test
  fun `fails if activity category is 'not in work'`() {
    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    activity.activityCategory = activityCategory(code = "SAA_NOT_IN_WORK")

    assertThatThrownBy {
      service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test")
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add prisoner to the waiting list because the activity category is 'not in work'")
  }

  @Test
  fun `fails if activity has ended or ends today`() {
    activity.endDate = LocalDate.now()

    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add prisoner to the waiting list for an activity ending on or before today")
  }

  @Test
  fun `fails if application date is in the future`() {
    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.tomorrow(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Application date cannot be not be in the future")
  }

  @Test
  fun `fails if pending application already exists for prisoner`() {
    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    mock<WaitingList> { on { status } doReturn WaitingListStatus.PENDING }.also { pending ->
      waitingListRepository.stub {
        on {
          findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
            DEFAULT_CASELOAD_PENTONVILLE,
            "123456",
            schedule,
          )
        } doReturn listOf(pending)
      }
    }

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add prisoner to the waiting list because a pending application already exists")
  }

  @Test
  fun `fails if approved application already exists for prisoner`() {
    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
    )

    mock<WaitingList> { on { status } doReturn WaitingListStatus.APPROVED }.also { approved ->
      waitingListRepository.stub {
        on {
          findByPrisonCodeAndPrisonerNumberAndActivitySchedule(
            DEFAULT_CASELOAD_PENTONVILLE,
            "123456",
            schedule,
          )
        } doReturn listOf(approved)
      }
    }

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add prisoner to the waiting list because an approved application already exists")
  }

  @Test
  fun `fails if prisoner already allocated (in future)`() {
    val request = WaitingListApplicationRequest(
      prisonerNumber = "123456",
      activityScheduleId = schedule.activityScheduleId,
      applicationDate = TimeSource.today(),
      requestedBy = "Test",
      status = WaitingListStatus.PENDING,
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
          DEFAULT_CASELOAD_PENTONVILLE,
          "123456",
          schedule,
        )
      } doReturn emptyList()
    }

    assertThatThrownBy { service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "test") }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Cannot add prisoner to the waiting list because they are already allocated")
  }

  @Test
  fun `add prisoner to waiting list succeeds`() {
    prisonApiClient.stub {
      on { getPrisonerDetails(prisonerNumber = "123456") } doReturn Mono.just(
        InmateDetailFixture.instance(
          offenderNo = "123456",
          status = "ACTIVE IN",
          agencyId = DEFAULT_CASELOAD_PENTONVILLE,
          bookingId = 1L,
        ),
      )

      val request = WaitingListApplicationRequest(
        prisonerNumber = "123456",
        activityScheduleId = 1L,
        applicationDate = TimeSource.today(),
        requestedBy = "Bob",
        comments = "Bob's comments",
        status = WaitingListStatus.PENDING,
      )

      service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "Test user")

      verify(waitingListRepository).saveAndFlush(waitingListCaptor.capture())

      with(waitingListCaptor.firstValue) {
        prisonerNumber isEqualTo "123456"
        activitySchedule isEqualTo schedule
        applicationDate isEqualTo TimeSource.today()
        requestedBy isEqualTo "Bob"
        comments isEqualTo "Bob's comments"
        status isEqualTo WaitingListStatus.PENDING
      }
    }
  }

  @Test
  fun `can add multiple waiting list records for the same prisoner`() {
    prisonApiClient.stub {
      on { getPrisonerDetails(prisonerNumber = "654321") } doReturn Mono.just(
        InmateDetailFixture.instance(
          offenderNo = "654321",
          status = "ACTIVE IN",
          agencyId = DEFAULT_CASELOAD_PENTONVILLE,
          bookingId = 1L,
        ),
      )

      val request = WaitingListApplicationRequest(
        prisonerNumber = "654321",
        activityScheduleId = 1L,
        applicationDate = TimeSource.today(),
        requestedBy = "Bob",
        comments = "Bob's comments",
        status = WaitingListStatus.DECLINED,
      )

      service.addPrisoner(DEFAULT_CASELOAD_PENTONVILLE, request, "Test user")
      service.addPrisoner(
        DEFAULT_CASELOAD_PENTONVILLE,
        request.copy(
          applicationDate = TimeSource.yesterday(),
          requestedBy = "Fred",
          comments = "Fred's comments",
          status = WaitingListStatus.PENDING,
        ),
        "Test user",
      )

      verify(waitingListRepository, times(2)).saveAndFlush(waitingListCaptor.capture())

      with(waitingListCaptor.firstValue) {
        prisonerNumber isEqualTo "654321"
        activitySchedule isEqualTo schedule
        applicationDate isEqualTo TimeSource.today()
        requestedBy isEqualTo "Bob"
        comments isEqualTo "Bob's comments"
        status isEqualTo WaitingListStatus.DECLINED
      }

      with(waitingListCaptor.secondValue) {
        prisonerNumber isEqualTo "654321"
        activitySchedule isEqualTo schedule
        applicationDate isEqualTo TimeSource.yesterday()
        requestedBy isEqualTo "Fred"
        comments isEqualTo "Fred's comments"
        status isEqualTo WaitingListStatus.PENDING
      }
    }
  }

  @Test
  fun `get waiting lists by the schedule identifier`() {
    val schedule = activityEntity(prisonCode = pentonvillePrisonCode).schedules().first()
    val allocation = schedule.allocations().first()

    val waitingList = WaitingList(
      waitingListId = 99,
      prisonCode = pentonvillePrisonCode,
      activitySchedule = schedule,
      prisonerNumber = "123456",
      bookingId = 100L,
      applicationDate = TimeSource.today(),
      requestedBy = "Fred",
      comments = "Some random test comments",
      status = WaitingListStatus.DECLINED,
      createdBy = "Bob",
    ).apply {
      this.allocation = allocation
      this.updatedBy = "Test"
      this.updatedTime = TimeSource.now()
      this.declinedReason = "Needs to attend level one activity first"
    }

    scheduleRepository.stub {
      on { scheduleRepository.findById(1L) } doReturn Optional.of(schedule)
    }

    waitingListRepository.stub {
      on { findByActivitySchedule(schedule) } doReturn listOf(waitingList)
    }

    with(service.getWaitingListsBySchedule(1L)) {
      assertThat(size).isEqualTo(1)

      with(first()) {
        id isEqualTo 99
        scheduleId isEqualTo schedule.activityScheduleId
        allocationId isEqualTo allocation.allocationId
        prisonCode isEqualTo pentonvillePrisonCode
        prisonerNumber isEqualTo "123456"
        bookingId isEqualTo 100L
        status isEqualTo WaitingListStatus.DECLINED
        requestedDate isEqualTo TimeSource.today()
        requestedBy isEqualTo "Fred"
        comments isEqualTo "Some random test comments"
        createdBy isEqualTo "Bob"
        creationTime isCloseTo TimeSource.now()
        updatedBy isEqualTo "Test"
        updatedTime!! isCloseTo TimeSource.now()
        declinedReason isEqualTo "Needs to attend level one activity first"
      }
    }
  }

  @Test
  fun `get waiting lists by the schedule identifier fails when not found`() {
    whenever(scheduleRepository.findById(any())) doReturn Optional.empty()

    assertThatThrownBy { service.getWaitingListsBySchedule(99) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Activity Schedule 99 not found")
  }

  @Test
  fun `get waiting list by id`() {
    val waitingList = waitingList(pentonvillePrisonCode).copy(waitingListId = 99)

    whenever(waitingListRepository.findById(waitingList.waitingListId)) doReturn Optional.of(waitingList)

    service.getWaitingListBy(99) isEqualTo waitingList.toModel()
  }

  @Test
  fun `get waiting list by id fails if does not have case load access`() {
    whenever(waitingListRepository.findById(99)) doReturn Optional.of(waitingList("WRONG_CASELOAD").copy(waitingListId = 99))

    assertThatThrownBy { service.getWaitingListBy(99) }.isInstanceOf(CaseloadAccessException::class.java)
  }

  @Test
  fun `get waiting list by id fails if not found`() {
    whenever(waitingListRepository.findById(any())) doReturn Optional.empty()

    assertThatThrownBy { service.getWaitingListBy(99) }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Waiting List 99 not found")
  }

  @Test
  fun `update the application date`() {
    val waitingList = waitingList(pentonvillePrisonCode).copy(applicationDate = TimeSource.today()).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    waitingList.applicationDate isEqualTo TimeSource.today()
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(applicationDate = TimeSource.yesterday()),
      "Jon",
    )

    waitingList.applicationDate isEqualTo TimeSource.yesterday()
    waitingList.updatedTime!! isCloseTo TimeSource.now()
    waitingList.updatedBy!! isEqualTo "Jon"
  }

  @Test
  fun `update the application date no-op if the same`() {
    val waitingList = waitingList(pentonvillePrisonCode).copy(applicationDate = TimeSource.today()).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    waitingList.applicationDate isEqualTo TimeSource.today()
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(applicationDate = TimeSource.today()),
      "Jon",
    )

    waitingList.applicationDate isEqualTo TimeSource.today()
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
  }

  @Test
  fun `update the application date fails if after creation date`() {
    val waitingList = waitingList(pentonvillePrisonCode).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    waitingList.applicationDate isEqualTo TimeSource.today()

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(applicationDate = TimeSource.tomorrow()),
        "Fred",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The application date cannot be after the date the application was initially created ${TimeSource.today()}")
  }

  @Test
  fun `update requested by`() {
    val waitingList = waitingList(pentonvillePrisonCode).copy(requestedBy = "Fred").also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    waitingList.requestedBy isEqualTo "Fred"
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(requestedBy = "Bob"),
      "Fred",
    )

    waitingList.requestedBy isEqualTo "Bob"
    waitingList.updatedTime!! isCloseTo TimeSource.now()
    waitingList.updatedBy isEqualTo "Fred"
  }

  @Test
  fun `update requested by no-op if the same`() {
    val waitingList = waitingList(pentonvillePrisonCode).copy(requestedBy = "Fred").also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    waitingList.requestedBy isEqualTo "Fred"
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(requestedBy = "Fred"),
      "Fred",
    )

    waitingList.requestedBy isEqualTo "Fred"
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
  }

  @Test
  fun `update requested fails if blank or empty`() {
    val waitingList = waitingList(pentonvillePrisonCode).copy(requestedBy = "Fred").also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    waitingList.requestedBy isEqualTo "Fred"

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(requestedBy = " "),
        "Fred",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Requested by cannot be blank or empty")

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(requestedBy = ""),
        "Fred",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Requested by cannot be blank or empty")
  }

  @Test
  fun `update comments`() {
    val waitingList = waitingList(pentonvillePrisonCode).copy(comments = "Initial comments").also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    waitingList.comments isEqualTo "Initial comments"
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(comments = "Updated comments"),
      "Lucy",
    )

    waitingList.comments isEqualTo "Updated comments"
    waitingList.updatedTime!! isCloseTo TimeSource.now()
    waitingList.updatedBy isEqualTo "Lucy"
  }

  @Test
  fun `update comments no-op if the same`() {
    val waitingList = waitingList(pentonvillePrisonCode).copy(comments = "Initial comments").also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    waitingList.comments isEqualTo "Initial comments"
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(comments = "Initial comments"),
      "Lucy",
    )

    waitingList.comments isEqualTo "Initial comments"
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
  }

  @Test
  fun `update status`() {
    val waitingList = waitingList(pentonvillePrisonCode).copy(status = WaitingListStatus.PENDING).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(status = WaitingListStatus.APPROVED),
      "Frank",
    )

    waitingList.status isEqualTo WaitingListStatus.APPROVED
    waitingList.updatedTime!! isCloseTo TimeSource.now()
    waitingList.updatedBy isEqualTo "Frank"
  }

  @Test
  fun `update status no-op if the same`() {
    val waitingList = waitingList(pentonvillePrisonCode).copy(status = WaitingListStatus.PENDING).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null

    service.updateWaitingList(
      waitingList.waitingListId,
      WaitingListApplicationUpdateRequest(status = WaitingListStatus.PENDING),
      "Frank",
    )

    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
  }

  @Test
  fun `update status fails if already allocated`() {
    val waitingList = waitingList(pentonvillePrisonCode).copy(status = WaitingListStatus.ALLOCATED).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(status = WaitingListStatus.DECLINED),
        "Frank",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The waiting list ${waitingList.waitingListId} can no longer be updated")

    waitingList.status isEqualTo WaitingListStatus.ALLOCATED
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
  }

  @Test
  fun `update status fails if already removed`() {
    val waitingList = waitingList(pentonvillePrisonCode).copy(status = WaitingListStatus.REMOVED).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(status = WaitingListStatus.APPROVED),
        "Frank",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("The waiting list ${waitingList.waitingListId} can no longer be updated")

    waitingList.status isEqualTo WaitingListStatus.REMOVED
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
  }

  @Test
  fun `update status fails if not PENDING, APPROVED or DECLINED`() {
    val waitingList = waitingList(pentonvillePrisonCode).copy(status = WaitingListStatus.PENDING).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    assertThatThrownBy {
      service.updateWaitingList(
        waitingList.waitingListId,
        WaitingListApplicationUpdateRequest(status = WaitingListStatus.ALLOCATED),
        "Frank",
      )
    }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Only PENDING, APPROVED or DECLINED are allowed for the status change")

    waitingList.status isEqualTo WaitingListStatus.PENDING
    waitingList.updatedTime isEqualTo null
    waitingList.updatedBy isEqualTo null
  }

  @Test
  fun `update fails if waiting list not found`() {
    whenever(waitingListRepository.findById(any())) doReturn Optional.empty()

    assertThatThrownBy {
      service.updateWaitingList(99, WaitingListApplicationUpdateRequest(status = WaitingListStatus.ALLOCATED), "Frank")
    }
      .isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("Waiting List 99 not found")
  }

  @Test
  fun `update fails if incorrect caseload`() {
    val waitingList = waitingList(moorlandPrisonCode).copy(status = WaitingListStatus.PENDING).also {
      whenever(waitingListRepository.findById(it.waitingListId)) doReturn Optional.of(it)
    }

    assertThatThrownBy {
      service.updateWaitingList(waitingList.waitingListId, WaitingListApplicationUpdateRequest(status = WaitingListStatus.ALLOCATED), "Frank")
    }
      .isInstanceOf(CaseloadAccessException::class.java)
  }
}
