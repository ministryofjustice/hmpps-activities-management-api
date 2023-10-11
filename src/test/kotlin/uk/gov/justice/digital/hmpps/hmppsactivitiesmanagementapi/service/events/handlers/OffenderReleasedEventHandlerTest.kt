package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.SentenceCalcDates
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AppointmentAttendeeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ReleaseInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReleasedEvent
import java.time.LocalDate

class OffenderReleasedEventHandlerTest {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock {
    on { findByCode(moorlandPrisonCode) } doReturn
      rolloutPrison().copy(
        activitiesToBeRolledOut = true,
        activitiesRolloutDate = LocalDate.now().plusDays(-1),
      )
  }
  private val prisonApiClient: PrisonApiApplicationClient = mock()
  private val appointmentAttendeeService: AppointmentAttendeeService = mock()
  private val prisonerAllocationHandler: PrisonerAllocationHandler = mock()
  private val allocationRepository: AllocationRepository = mock()

  private val handler = OffenderReleasedEventHandler(
    rolloutPrisonRepository,
    appointmentAttendeeService,
    prisonApiClient,
    prisonerAllocationHandler,
    allocationRepository,
  )

  private val prisoner: InmateDetail = mock {
    on { status } doReturn "INACTIVE OUT"
  }

  @BeforeEach
  fun beforeEach() {
    whenever(allocationRepository.existAtPrisonForPrisoner(any(), any(), eq(PrisonerStatus.allExcuding(PrisonerStatus.ENDED).toList()))) doReturn true
  }

  @Test
  fun `inbound released event is not handled for an inactive prison`() {
    reset(rolloutPrisonRepository)
    rolloutPrisonRepository.stub {
      on { findByCode(moorlandPrisonCode) } doReturn
        rolloutPrison().copy(
          activitiesToBeRolledOut = false,
          activitiesRolloutDate = null,
        )
    }

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456")).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(moorlandPrisonCode)
    verifyNoInteractions(prisonerAllocationHandler)
  }

  @Test
  fun `inbound release event is not processed when no matching prison is found`() {
    reset(rolloutPrisonRepository)
    rolloutPrisonRepository.stub { on { findByCode(moorlandPrisonCode) } doReturn null }

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456")).also { it.isSuccess() isBool true }

    verify(rolloutPrisonRepository).findByCode(moorlandPrisonCode)
    verifyNoInteractions(prisonerAllocationHandler)
  }

  @Test
  fun `pending allocations are removed and un-ended allocations are ended on release from remand`() {
    val sentenceCalcDatesNoReleaseDateForRemand: SentenceCalcDates = mock { on { releaseDate } doReturn null }

    prisoner.stub {
      on { sentenceDetail } doReturn sentenceCalcDatesNoReleaseDateForRemand
    }

    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = true, extraInfo = true)) doReturn Mono.just(
      prisoner,
    )

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456")).also { it.isSuccess() isBool true }

    verify(prisonerAllocationHandler).deallocate(moorlandPrisonCode, "123456", DeallocationReason.RELEASED)
  }

  @Test
  fun `un-ended allocations are ended on release from custodial`() {
    val sentenceCalcDatesReleaseDateTodayForCustodialSentence: SentenceCalcDates =
      mock { on { releaseDate } doReturn LocalDate.now() }

    prisoner.stub {
      on { sentenceDetail } doReturn sentenceCalcDatesReleaseDateTodayForCustodialSentence
    }

    whenever(prisonApiClient.getPrisonerDetails("123456", fullInfo = true, extraInfo = true))
      .doReturn(Mono.just(prisoner))

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456")).also { it.isSuccess() isBool true }

    verify(prisonerAllocationHandler).deallocate(moorlandPrisonCode, "123456", DeallocationReason.RELEASED)
  }

  @Test
  fun `allocation is unmodified for unknown release event`() {
    val outcome = handler.handle(
      OffenderReleasedEvent(
        ReleaseInformation(
          nomsNumber = "12345",
          reason = "UNKNOWN",
          prisonId = moorlandPrisonCode,
        ),
      ),
    )

    assertThat(outcome.isSuccess()).isFalse
  }

  @Test
  fun `all future allocations are cancelled for a released event`() {
    val prisonerNumber = "12345"
    whenever(prisonApiClient.getPrisonerDetails(prisonerNumber, fullInfo = true, extraInfo = true))
      .doReturn(Mono.just(prisoner))

    val outcome = handler.handle(
      OffenderReleasedEvent(
        ReleaseInformation(
          nomsNumber = prisonerNumber,
          reason = "RELEASED",
          prisonId = moorlandPrisonCode,
        ),
      ),
    )

    assertThat(outcome.isSuccess()).isTrue()
    verify(appointmentAttendeeService).cancelFutureOffenderAppointments(moorlandPrisonCode, "12345")
  }

  @Test
  fun `no interactions when released prisoner has no allocations of interest`() {
    whenever(allocationRepository.existAtPrisonForPrisoner(any(), any(), eq(PrisonerStatus.allExcuding(PrisonerStatus.ENDED).toList()))) doReturn false

    handler.handle(offenderReleasedEvent(moorlandPrisonCode, "123456")).also { it.isSuccess() isBool true }

    verifyNoInteractions(prisonApiClient)
    verifyNoInteractions(prisonerAllocationHandler)
  }
}
