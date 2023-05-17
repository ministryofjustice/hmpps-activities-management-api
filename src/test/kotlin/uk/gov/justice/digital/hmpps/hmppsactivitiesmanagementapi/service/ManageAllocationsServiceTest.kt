package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiApplicationClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.extensions.MovementType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.moorlandPrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonRegimeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.RolloutPrisonRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class ManageAllocationsServiceTest {

  private val rolloutPrisonRepo: RolloutPrisonRepository = mock()
  private val activityRepo: ActivityRepository = mock()
  private val activityScheduleRepo: ActivityScheduleRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val prisonRegimeRepository: PrisonRegimeRepository = mock()
  private val searchApiClient: PrisonerSearchApiApplicationClient = mock()

  private val service =
    ManageAllocationsService(
      rolloutPrisonRepo,
      activityRepo,
      activityScheduleRepo,
      allocationRepository,
      prisonRegimeRepository,
      searchApiClient,
    )
  private val yesterday = LocalDate.now().minusDays(1)
  private val today = yesterday.plusDays(1)

  @Test
  fun `deallocate offenders from activity ending today`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = today)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.verifyIsActive() }

    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(prison))
    whenever(activityRepo.getAllForPrisonAndDate(prison.code, LocalDate.now())).thenReturn(listOf(activity))

    service.allocations(AllocationOperation.DEALLOCATE_ENDING)

    allocation.verifyIsEnded()

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `deallocate offenders from activity with no end date and allocation ends today`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = null)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().apply { endDate = today }.also { it.verifyIsActive() }

    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(prison))
    whenever(activityRepo.getAllForPrisonAndDate(prison.code, LocalDate.now())).thenReturn(listOf(activity))

    service.allocations(AllocationOperation.DEALLOCATE_ENDING)

    allocation.verifyIsEnded()

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `offenders not deallocated from activity with no end date and allocation does not end today`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = null)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().also { it.verifyIsActive() }

    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(prison))
    whenever(activityRepo.getAllForPrisonAndDate(prison.code, LocalDate.now())).thenReturn(listOf(activity))

    service.allocations(AllocationOperation.DEALLOCATE_ENDING)

    allocation.verifyIsActive()

    verify(activityScheduleRepo, never()).saveAndFlush(any())
  }

  @Test
  fun `deallocate offenders from activities across multiple prisons`() {
    val moorland = rolloutPrison().copy(code = moorlandPrisonCode)
    val pentonville = rolloutPrison()
    whenever(rolloutPrisonRepo.findAll()).thenReturn(listOf(pentonville, moorland))

    val pentonvilleActivity = activityEntity(activityId = 1, startDate = yesterday, endDate = today)
    val moorlandActivity = activityEntity(activityId = 2, startDate = yesterday, endDate = today)
    whenever(activityRepo.getAllForPrisonAndDate(pentonville.code, today)).thenReturn(listOf(pentonvilleActivity))
    whenever(activityRepo.getAllForPrisonAndDate(moorland.code, today)).thenReturn(listOf(moorlandActivity))

    service.allocations(AllocationOperation.DEALLOCATE_ENDING)

    listOf(pentonvilleActivity, moorlandActivity).onEach { activity ->
      with(activity) {
        this.schedules().first().allocations().forEach { assertThat(it.status(PrisonerStatus.ENDED)).isTrue() }
        verify(activityScheduleRepo).saveAndFlush(this.schedules().first())
      }
    }
  }

  @Test
  fun `temporarily absent offenders are deallocated from allocations due to expire`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = today)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().autoSuspend(LocalDateTime.now().minusDays(5), "reason")
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.oUT
      on { prisonerNumber } doReturn allocation.prisonerNumber
      on { lastMovementTypeCode } doReturn MovementType.TEMPORARY_ABSENCE.nomisShortCode
    }

    whenever(rolloutPrisonRepo.findAll()).doReturn(listOf(prison))
    whenever(prisonRegimeRepository.findByPrisonCode(prison.code)).doReturn(prisonRegime())
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.code, PrisonerStatus.AUTO_SUSPENDED)).doReturn(
      listOf(allocation),
    )
    whenever(searchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))).doReturn(Mono.just(listOf(prisoner)))

    service.allocations(AllocationOperation.DEALLOCATE_EXPIRING)

    allocation.verifyIsExpired()

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `transferred offenders are deallocated from allocations due to expire`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = today)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().autoSuspend(LocalDateTime.now().minusDays(5), "reason")
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.oUT
      on { prisonerNumber } doReturn allocation.prisonerNumber
      on { lastMovementTypeCode } doReturn MovementType.TRANSFER.nomisShortCode
    }

    whenever(rolloutPrisonRepo.findAll()).doReturn(listOf(prison))
    whenever(prisonRegimeRepository.findByPrisonCode(prison.code)).doReturn(prisonRegime())
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.code, PrisonerStatus.AUTO_SUSPENDED)).doReturn(
      listOf(allocation),
    )
    whenever(searchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))).doReturn(Mono.just(listOf(prisoner)))

    service.allocations(AllocationOperation.DEALLOCATE_EXPIRING)

    allocation.verifyIsExpired()

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  @Test
  fun `released offenders are deallocated from allocations due to expire`() {
    val prison = rolloutPrison()
    val activity = activityEntity(startDate = yesterday, endDate = today)
    val schedule = activity.schedules().first()
    val allocation = schedule.allocations().first().autoSuspend(LocalDateTime.now().minusDays(5), "reason")
    val prisoner: Prisoner = mock {
      on { inOutStatus } doReturn Prisoner.InOutStatus.oUT
      on { prisonerNumber } doReturn allocation.prisonerNumber
      on { lastMovementTypeCode } doReturn MovementType.RELEASE.nomisShortCode
      on { releaseDate } doReturn LocalDate.now().minusDays(5)
    }

    whenever(rolloutPrisonRepo.findAll()).doReturn(listOf(prison))
    whenever(prisonRegimeRepository.findByPrisonCode(prison.code)).doReturn(prisonRegime())
    whenever(allocationRepository.findByPrisonCodePrisonerStatus(prison.code, PrisonerStatus.AUTO_SUSPENDED)).doReturn(
      listOf(allocation),
    )
    whenever(searchApiClient.findByPrisonerNumbers(listOf(prisoner.prisonerNumber))).doReturn(Mono.just(listOf(prisoner)))

    service.allocations(AllocationOperation.DEALLOCATE_EXPIRING)

    allocation.verifyIsExpired()

    verify(activityScheduleRepo).saveAndFlush(schedule)
  }

  private fun Allocation.verifyIsActive() {
    assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE)
  }

  private fun Allocation.verifyIsEnded() {
    assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ENDED)
    assertThat(deallocatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(deallocatedReason).isEqualTo("Allocation end date reached")
    assertThat(deallocatedBy).isNotNull
  }

  private fun Allocation.verifyIsExpired() {
    assertThat(prisonerStatus).isEqualTo(PrisonerStatus.ENDED)
    assertThat(deallocatedTime).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
    assertThat(deallocatedReason).isEqualTo("Expired")
    assertThat(deallocatedBy).isNotNull
  }
}
