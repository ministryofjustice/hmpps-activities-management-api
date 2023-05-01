package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.handlers

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.InmateDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventReview
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.cellMoveEvent
import java.time.LocalDate

class InterestingEventHandlerTest {
  private val rolloutPrisonRepository: RolloutPrisonRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val eventReviewRepository: EventReviewRepository = mock()
  private val prisonApiClient: PrisonApiApplicationClient = mock()

  private val handler = InterestingEventHandler(rolloutPrisonRepository, allocationRepository, eventReviewRepository, prisonApiClient)

  private val prisoner = InmateDetail(
    agencyId = "PVI",
    offenderNo = "123456",
    inOutStatus = InmateDetail.InOutStatus.IN,
    firstName = "Bob",
    lastName = "Bobson",
    activeFlag = true,
    offenderId = 1L,
    rootOffenderId = 1L,
    status = InmateDetail.Status.IN,
    dateOfBirth = LocalDate.of(2001, 10, 1),
  )

  @BeforeEach
  fun beforeTests() {
    reset(rolloutPrisonRepository, allocationRepository, eventReviewRepository, prisonApiClient)
    rolloutPrisonRepository.stub {
      on { findByCode(pentonvillePrisonCode) } doReturn rolloutPrison()
    }
    whenever(prisonApiClient.getPrisonerDetails("123456")).doReturn(Mono.just(prisoner))
    whenever(eventReviewRepository.saveAndFlush(any<EventReview>())).doReturn(EventReview(eventReviewId = 1))
  }

  @Test
  fun `stores an interesting event when allocations exist`() {
    val inboundEvent = cellMoveEvent("123456")
    val activeAllocations = listOf(allocation().copy(allocationId = 1, prisonerNumber = "123456"))
    whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "123456"))
      .doReturn(activeAllocations)

    val result = handler.handle(inboundEvent)

    assertThat(result).isTrue
    verify(rolloutPrisonRepository, times(1)).findByCode(pentonvillePrisonCode)
    verify(allocationRepository, times(1)).findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "123456")
    verify(eventReviewRepository, times(1)).saveAndFlush(any<EventReview>())
  }

  @Test
  fun `ignores prisoners in prisons which are not rolled out`() {
    val inboundEvent = cellMoveEvent("123456")
    rolloutPrisonRepository.stub {
      on { findByCode(pentonvillePrisonCode) } doReturn
        rolloutPrison().copy(
          code = pentonvillePrisonCode,
          activitiesToBeRolledOut = false,
          activitiesRolloutDate = null,
        )
    }

    val result = handler.handle(inboundEvent)

    assertThat(result).isFalse
    verify(rolloutPrisonRepository).findByCode(pentonvillePrisonCode)
    verifyNoInteractions(allocationRepository)
    verifyNoInteractions(eventReviewRepository)
  }

  @Test
  fun `ignores events for a prisoner with no active allocations`() {
    val inboundEvent = cellMoveEvent("123456")
    whenever(allocationRepository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "123456"))
      .doReturn(emptyList())

    val result = handler.handle(inboundEvent)

    assertThat(result).isFalse
    verify(rolloutPrisonRepository, times(1)).findByCode(pentonvillePrisonCode)
    verify(allocationRepository, times(1)).findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "123456")
    verifyNoInteractions(eventReviewRepository)
  }
}
