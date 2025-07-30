package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Pageable
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.api.CaseNotesApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.casenotesapi.model.CaseNote
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api.NonAssociationsApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.NonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.OtherPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Education
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PagedPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerAlert
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.containsExactly
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.notInWorkCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.schedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCandidate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.EarliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.AllocationPayRate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.EducationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.IncentiveLevelSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.ReleaseDateSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.WRASuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.CandidateAllocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.OtherPrisonerDetails as OtherPrisonerDetailsDto

class CandidatesServiceTest {
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val nonAssociationsApiClient: NonAssociationsApiClient = mock()
  private val caseNotesApiClient: CaseNotesApiClient = mock()
  private val activityScheduleRepository: ActivityScheduleRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val waitingListRepository: WaitingListRepository = mock()
  private val pageable = Pageable.ofSize(20).withPage(0)

  private val service = CandidatesService(
    prisonApiClient,
    prisonerSearchApiClient,
    nonAssociationsApiClient,
    caseNotesApiClient,
    activityScheduleRepository,
    allocationRepository,
    waitingListRepository,
  )

  @Nested
  inner class CandidateSuitability {
    private val candidateEducation = Education(
      bookingId = 1,
      studyArea = "English Language",
      educationLevel = "Reading Measure 1.0",
    )

    private fun candidateSuitabilitySetup(activity: Activity, candidate: Prisoner) {
      val schedule = activity.schedules().first()

      whenever(activityScheduleRepository.findById(1)).thenReturn(Optional.of(schedule))
      whenever(prisonerSearchApiClient.findByPrisonerNumber(candidate.prisonerNumber)).thenReturn(candidate)
      whenever(prisonApiClient.getEducationLevels(listOf(candidate.prisonerNumber))).thenReturn(
        listOf(candidateEducation),
      )
      whenever(nonAssociationsApiClient.getOffenderNonAssociations(candidate.prisonerNumber)).thenReturn(emptyList())

      whenever(
        allocationRepository.findByPrisonCodeAndPrisonerNumber(
          candidate.prisonId!!,
          candidate.prisonerNumber,
        ),
      ).thenReturn(
        listOf(
          allocation().copy(allocationId = 1, prisonerNumber = candidate.prisonerNumber),
          allocation().copy(allocationId = 2, prisonerNumber = candidate.prisonerNumber).apply {
            deallocateOn(LocalDate.now(), DeallocationReason.SECURITY, ServiceName.SERVICE_NAME.toString(), UUID.fromString("8db661aa-4867-4ed4-9ac4-5f0f01e26c22"))
            deallocateNowOn(TimeSource.today())
          },
        ),
      )
      whenever(caseNotesApiClient.getCaseNote(candidate.prisonerNumber, UUID.fromString("8db661aa-4867-4ed4-9ac4-5f0f01e26c22"))).thenReturn(
        CaseNote(
          caseNoteId = "10001",
          offenderIdentifier = candidate.prisonerNumber,
          type = "NEG",
          typeDescription = "Negative Behaviour",
          subType = "NEG_GEN",
          subTypeDescription = "General Entry",
          source = "INST",
          creationDateTime = LocalDateTime.now(),
          occurrenceDateTime = LocalDateTime.now(),
          authorName = "Test",
          authorUserId = "1",
          authorUsername = "test_1",
          text = "Test case note",
          eventId = 1,
          sensitive = false,
        ),
      )
    }

    @Test
    fun `fails if schedule not found`() {
      assertThatThrownBy {
        service.candidateSuitability(1, "A1234AA")
      }.isInstanceOf(EntityNotFoundException::class.java)
    }

    @Test
    fun `wraSuitability - suitable`() {
      val activity = activityEntity(riskLevel = "high")
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        alerts = listOf(
          PrisonerAlert(
            alertType = "R",
            alertCode = "RHI",
            active = true,
            expired = false,
          ),
        ),
      )

      candidateSuitabilitySetup(activity, candidate)
      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.workplaceRiskAssessment).isEqualTo(
        WRASuitability(
          suitable = true,
          riskLevel = "high",
        ),
      )
    }

    @Test
    fun `wraSuitability - not suitable`() {
      val activity = activityEntity(riskLevel = "medium")
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        alerts = listOf(
          PrisonerAlert(
            alertType = "R",
            alertCode = "RHI",
            active = true,
            expired = false,
          ),
        ),
      )

      candidateSuitabilitySetup(activity, candidate)
      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.workplaceRiskAssessment).isEqualTo(
        WRASuitability(
          suitable = false,
          riskLevel = "high",
        ),
      )
    }

    @Test
    fun `wraSuitability - not suitable (no WRA & medium activity risk level)`() {
      val activity = activityEntity(riskLevel = "medium")
      val candidate = PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC")

      candidateSuitabilitySetup(activity, candidate)
      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.workplaceRiskAssessment).isEqualTo(
        WRASuitability(
          suitable = false,
          riskLevel = "none",
        ),
      )
    }

    @Test
    fun `wraSuitability - not suitable (no WRA & high activity risk level)`() {
      val activity = activityEntity(riskLevel = "high")
      val candidate = PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC")

      candidateSuitabilitySetup(activity, candidate)
      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.workplaceRiskAssessment).isEqualTo(
        WRASuitability(
          suitable = true,
          riskLevel = "none",
        ),
      )
    }

    @Test
    fun `incentiveLevelSuitability - suitable`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC")

      candidateSuitabilitySetup(activity, candidate)
      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.incentiveLevel).isEqualTo(
        IncentiveLevelSuitability(
          suitable = true,
          incentiveLevel = "Basic",
        ),
      )
    }

    @Test
    fun `incentiveLevelSuitability (Zero pay activity) - suitable`() {
      val activity = activityEntity(paid = false, noPayBands = true)
      val candidate = PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC")

      candidateSuitabilitySetup(activity, candidate)
      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.incentiveLevel).isEqualTo(
        IncentiveLevelSuitability(
          suitable = true,
          incentiveLevel = "Basic",
        ),
      )
    }

    @Test
    fun `incentiveLevelSuitability - not suitable`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        currentIncentive = CurrentIncentive(
          level = IncentiveLevel("Standard", "STD"),
          dateTime = "2020-07-20T10:36:53",
          nextReviewDate = LocalDate.of(2021, 7, 20),
        ),
      )

      candidateSuitabilitySetup(activity, candidate)
      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.incentiveLevel).isEqualTo(
        IncentiveLevelSuitability(
          suitable = false,
          incentiveLevel = "Standard",
        ),
      )
    }

    @Test
    fun `educationSuitability - suitable`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC")

      candidateSuitabilitySetup(activity, candidate)
      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.education).isEqualTo(
        EducationSuitability(
          suitable = true,
          education = listOf(candidateEducation),
        ),
      )
    }

    @Test
    fun `educationSuitability - not suitable`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC")

      candidateSuitabilitySetup(activity, candidate)

      val candidateEducation = candidateEducation.copy(educationLevel = "Reading Measure 2.0")

      whenever(prisonApiClient.getEducationLevels(listOf(candidate.prisonerNumber))).thenReturn(
        listOf(candidateEducation),
      )

      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.education).isEqualTo(
        EducationSuitability(
          suitable = false,
          education = listOf(candidateEducation),
        ),
      )
    }

    @Test
    fun `releaseDate - suitable`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        releaseDate = LocalDate.now().plusYears(1),
      )

      candidateSuitabilitySetup(activity, candidate)
      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.releaseDate).isEqualTo(
        ReleaseDateSuitability(
          suitable = true,
          earliestReleaseDate = EarliestReleaseDate(
            releaseDate = LocalDate.now().plusYears(1),
          ),
        ),
      )
    }

    @Test
    fun `releaseDate - not suitable`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        releaseDate = LocalDate.now().minusDays(1),
      )

      candidateSuitabilitySetup(activity, candidate)
      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.releaseDate).isEqualTo(
        ReleaseDateSuitability(
          suitable = false,
          earliestReleaseDate = EarliestReleaseDate(
            LocalDate.now().minusDays(1),
          ),
        ),
      )
    }

    @Test
    fun `releaseDate - not suitable (no release date)`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC")

      candidateSuitabilitySetup(activity, candidate)
      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.releaseDate).isEqualTo(
        ReleaseDateSuitability(
          suitable = true,
          earliestReleaseDate = EarliestReleaseDate(null),
        ),
      )
    }

    @Test
    fun `prisoner allocations`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC")

      candidateSuitabilitySetup(activity, candidate)

      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      val candidateAllocation = allocation().copy(allocationId = 1, prisonerNumber = "A1234BC")

      assertThat(suitability.allocations).containsOnly(
        AllocationPayRate(
          allocation = candidateAllocation.toModel(),
          payRate = candidateAllocation.allocationPay("BAS")?.toModelLite(),
        ),
      )
    }

    @Test
    fun `previous deallocations`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC")

      candidateSuitabilitySetup(activity, candidate)

      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      suitability.previousDeallocations.size isEqualTo 1
      with(suitability.previousDeallocations.first()) {
        allocation.deallocatedReason isEqualTo DeallocationReason.SECURITY.toModel()
        caseNoteText isEqualTo "Test case note"
      }
    }
  }

  @Nested
  inner class GetActivityCandidates {
    inner class TestCandidate(val testId: Long, val testPrisonerNumber: String, val testCode: String, val scheduleId: Long) : CandidateAllocation {
      override fun getActivityScheduleId(): Long = scheduleId

      override fun getAllocationId(): Long = testId

      override fun getPrisonerNumber(): String = testPrisonerNumber

      override fun getCode(): String = testCode
    }

    private fun candidatesSetup(
      activity: Activity,
      candidates: PagedPrisoner,
      waitingList: List<WaitingList> = emptyList(),
      candidateAllocations: List<Allocation> = emptyList(),
    ) {
      addCaseloadIdToRequestHeader("MDI")

      val schedule = activity.schedules().first()

      whenever(activityScheduleRepository.findById(1)).thenReturn(Optional.of(schedule))
      whenever(waitingListRepository.findByActivitySchedule(schedule)).thenReturn(waitingList)
      whenever(prisonerSearchApiClient.getAllPrisonersInPrison(schedule.activity.prisonCode)).thenReturn(Mono.just(candidates))

      whenever(allocationRepository.findByAllocationIdIn(any())).thenReturn(candidateAllocations)
      whenever(allocationRepository.getCandidateAllocations(any())).thenReturn(
        candidateAllocations.map {
          TestCandidate(it.allocationId, it.prisonerNumber, it.activitySchedule.activity.activityCategory.code, 99)
        },
      )
    }

    @BeforeEach
    fun setUp() {
      nonAssociationsApiClient.stub {
        on {
          runBlocking {
            nonAssociationsApiClient.getNonAssociationsInvolving(anyString(), anyList())
          }
        } doReturn emptyList()
      }
    }

    @Test
    fun `fetch list of suitable candidates`() {
      val activity = activityEntity()
      val allPrisoners = PrisonerSearchPrisonerFixture.pagedResultWithSurnames(prisonerNumberAndSurnames = listOf("A1234BC" to "Harrison", "A1234BI" to "Allen", "D3333DD" to "Jones"))
      val waitingList = listOf(waitingList(prisonCode = activity.prisonCode, prisonerNumber = "A1234BC", initialStatus = WaitingListStatus.REMOVED))

      candidatesSetup(activity, allPrisoners, waitingList)

      val nonAssociation1: NonAssociation = mock {
        on { firstPrisonerNumber } doReturn "A1234BC"
      }

      val nonAssociation2: NonAssociation = mock {
        on { secondPrisonerNumber } doReturn "A1234BI"
      }

      nonAssociationsApiClient.stub {
        on {
          runBlocking {
            nonAssociationsApiClient.getNonAssociationsInvolving("MDI", listOf("A1234BI", "A1234BC", "D3333DD"))
          }
        } doReturn listOf(nonAssociation1, nonAssociation2)
      }

      val candidates = service.getActivityCandidates(
        activity.schedules().first().activityScheduleId,
        null,
        null,
        null,
        null,
        null,
        pageable,
      )

      assertThat(candidates.content).isEqualTo(
        listOf(
          ActivityCandidate(
            name = "Tim Allen",
            firstName = "Tim",
            lastName = "Allen",
            prisonerNumber = "A1234BI",
            cellLocation = "1-2-3",
            otherAllocations = emptyList(),
            earliestReleaseDate = EarliestReleaseDate(null),
            nonAssociations = true,
          ),
          ActivityCandidate(
            name = "Tim Harrison",
            firstName = "Tim",
            lastName = "Harrison",
            prisonerNumber = "A1234BC",
            cellLocation = "1-2-3",
            otherAllocations = emptyList(),
            earliestReleaseDate = EarliestReleaseDate(null),
            nonAssociations = true,
          ),
          ActivityCandidate(
            name = "Tim Jones",
            firstName = "Tim",
            lastName = "Jones",
            prisonerNumber = "D3333DD",
            cellLocation = "1-2-3",
            otherAllocations = emptyList(),
            earliestReleaseDate = EarliestReleaseDate(null),
            nonAssociations = false,
          ),
        ),
      )
    }

    @Test
    fun `fetch list of suitable candidates when non-associations returns null`() {
      val activity = activityEntity()
      val allPrisoners = PrisonerSearchPrisonerFixture.pagedResultWithSurnames(prisonerNumberAndSurnames = listOf("A1234BC" to "Harrison"))
      val waitingList = listOf(waitingList(prisonCode = activity.prisonCode, prisonerNumber = "A1234BC", initialStatus = WaitingListStatus.REMOVED))

      candidatesSetup(activity, allPrisoners, waitingList)

      nonAssociationsApiClient.stub {
        on {
          runBlocking {
            nonAssociationsApiClient.getNonAssociationsInvolving("MDI", listOf("A1234BC"))
          }
        } doReturn null
      }

      val candidates = service.getActivityCandidates(
        activity.schedules().first().activityScheduleId,
        null,
        null,
        null,
        null,
        null,
        pageable,
      )

      assertThat(candidates.content).isEqualTo(
        listOf(
          ActivityCandidate(
            name = "Tim Harrison",
            firstName = "Tim",
            lastName = "Harrison",
            prisonerNumber = "A1234BC",
            cellLocation = "1-2-3",
            otherAllocations = emptyList(),
            earliestReleaseDate = EarliestReleaseDate(null),
            nonAssociations = null,
          ),
        ),
      )
    }

    @Test
    fun `Employment filter should filter candidates without allocations or those with only 'not in work' activities when inWork == false`() {
      val activity = activityEntity()

      val notInWorkActivity = activityEntity(category = notInWorkCategory)
      val notInWorkAllocation = allocation().copy(
        prisonerNumber = "A1234BC",
        prisonerStatus = PrisonerStatus.ACTIVE,
        activitySchedule = notInWorkActivity.schedules().first(),
      )

      val inWorkAllocation = allocation().copy(prisonerNumber = "B2345CD")

      // A1234BC – Has an active allocation for a "not in work" activity
      // B2345CD – Has an active allocation that's not for a "not in work" activity
      // C3456DE – Has no active allocations
      val allPrisoners = PrisonerSearchPrisonerFixture.pagedResult(
        prisonerNumbers = listOf(
          "A1234BC",
          "B2345CD",
          "C3456DE",
        ),
      )

      candidatesSetup(activity, allPrisoners, candidateAllocations = listOf(notInWorkAllocation, inWorkAllocation))

      val candidates = service.getActivityCandidates(
        activity.schedules().first().activityScheduleId,
        null,
        null,
        false,
        null,
        null,
        pageable,
      )

      candidates.content.map { it.prisonerNumber } containsExactly listOf("A1234BC", "C3456DE")
    }

    @Test
    fun `Employment filter should filter candidates with allocations that are not for 'not in work' activities when inWork == true`() {
      val activity = activityEntity()

      val notInWorkActivity = activityEntity(category = notInWorkCategory)
      val notInWorkAllocation = allocation().copy(
        prisonerNumber = "A1234BC",
        prisonerStatus = PrisonerStatus.ACTIVE,
        activitySchedule = notInWorkActivity.schedules().first(),
      )

      val inWorkAllocation = allocation().copy(prisonerNumber = "B2345CD")

      // A1234BC – Has an active allocation for a "not in work" activity
      // B2345CD – Has an active allocation that's not for a "not in work" activity
      // C3456DE – Has no active allocations
      val allPrisoners = PrisonerSearchPrisonerFixture.pagedResult(
        prisonerNumbers = listOf(
          "A1234BC",
          "B2345CD",
          "C3456DE",
        ),
      )

      candidatesSetup(activity, allPrisoners, candidateAllocations = listOf(notInWorkAllocation, inWorkAllocation))

      val candidates = service.getActivityCandidates(
        activity.schedules().first().activityScheduleId,
        null,
        null,
        suitableForEmployed = true,
        noAllocations = false,
        search = null,
        pageable = pageable,
      )

      candidates.content.map { it.prisonerNumber } containsExactly listOf("B2345CD")
    }

    @Test
    fun `No allocations filter should filter candidates with allocations when noAllocations=true`() {
      val activity = activityEntity()

      val allocation = allocation().copy(prisonerNumber = "B2345CD")

      // A1234BC – Has no allocations
      // B2345CD – Has allocations
      // C3456DE – Has no allocations
      val allPrisoners = PrisonerSearchPrisonerFixture.pagedResult(
        prisonerNumbers = listOf(
          "A1234BC",
          "B2345CD",
          "C3456DE",
        ),
      )

      candidatesSetup(activity, allPrisoners, candidateAllocations = listOf(allocation))

      val candidates = service.getActivityCandidates(
        activity.schedules().first().activityScheduleId,
        null,
        null,
        suitableForEmployed = false,
        noAllocations = true,
        search = null,
        pageable = pageable,
      )

      candidates.content.map { it.prisonerNumber } containsExactly listOf("A1234BC", "C3456DE")
    }

    @Test
    fun `Candidates with PENDING waiting list are filtered out`() {
      val activity = activityEntity()
      val allPrisoners = PrisonerSearchPrisonerFixture.pagedResult(prisonerNumbers = listOf("A1234BC"))
      val waitingList = listOf(waitingList(prisonCode = activity.prisonCode, prisonerNumber = "A1234BC", initialStatus = WaitingListStatus.PENDING))

      candidatesSetup(activity, allPrisoners, waitingList)

      val candidates = service.getActivityCandidates(
        activity.schedules().first().activityScheduleId,
        null,
        null,
        null,
        null,
        null,
        pageable,
      )

      assertThat(candidates.content).isEmpty()
    }

    @Test
    fun `Candidates with APPROVED waiting list are filtered out`() {
      val activity = activityEntity()
      val allPrisoners = PrisonerSearchPrisonerFixture.pagedResult(prisonerNumbers = listOf("A1234BC"))
      val waitingList = listOf(waitingList(prisonCode = activity.prisonCode, prisonerNumber = "A1234BC", initialStatus = WaitingListStatus.APPROVED))

      candidatesSetup(activity, allPrisoners, waitingList)

      val candidates = service.getActivityCandidates(
        activity.schedules().first().activityScheduleId,
        null,
        null,
        null,
        null,
        null,
        pageable,
      )

      assertThat(candidates.content).isEmpty()
    }

    @Test
    fun `Candidates ENDED other allocations are filtered out`() {
      val schedule = activitySchedule(activityEntity(), noAllocations = true).apply {
        allocatePrisoner(
          prisonerNumber = "A1234BC".toPrisonerNumber(),
          payBand = lowPayBand,
          bookingId = 1L,
          startDate = TimeSource.tomorrow(),
          allocatedBy = "Test",
        ).deallocateNowOn(TimeSource.today())
      }.also { it.allocations().single().prisonerStatus isEqualTo PrisonerStatus.ENDED }

      candidatesSetup(schedule.activity, PrisonerSearchPrisonerFixture.pagedResult(prisonerNumbers = listOf("A1234BC")))

      val candidates = service.getActivityCandidates(
        schedule.activityScheduleId,
        null,
        null,
        null,
        null,
        null,
        pageable,
      )

      candidates.content.single().otherAllocations.isEmpty() isBool true
    }

    @Test
    fun `Candidates PENDING other allocations are not filtered out`() {
      val schedule = activitySchedule(activityEntity(), noAllocations = true).apply {
        allocatePrisoner(
          prisonerNumber = "A1234BC".toPrisonerNumber(),
          payBand = lowPayBand,
          bookingId = 1L,
          startDate = TimeSource.tomorrow(),
          allocatedBy = "Test",
        )
      }.also { it.allocations().single().prisonerStatus isEqualTo PrisonerStatus.PENDING }

      candidatesSetup(schedule.activity, PrisonerSearchPrisonerFixture.pagedResult(prisonerNumbers = listOf("A1234BC")))
      whenever(allocationRepository.findByAllocationIdIn(any())).thenReturn(schedule.allocations())
      whenever(allocationRepository.getCandidateAllocations(any())).thenReturn(
        schedule.allocations().map {
          TestCandidate(it.allocationId, it.prisonerNumber, it.activitySchedule.activity.activityCategory.code, 99)
        },
      )

      val candidates = service.getActivityCandidates(
        schedule.activityScheduleId,
        null,
        null,
        null,
        null,
        null,
        pageable,
      )

      candidates.content.single().otherAllocations.single() isEqualTo schedule.allocations().single().toModel()
    }

    @Test
    fun `Candidates SUSPENDED other allocations are not filtered out`() {
      val schedule = activitySchedule(activityEntity(), noAllocations = true).apply {
        allocatePrisoner(
          prisonerNumber = "A1234BC".toPrisonerNumber(),
          payBand = lowPayBand,
          bookingId = 1L,
          startDate = TimeSource.today(),
          allocatedBy = "Test",
        ).autoSuspend(TimeSource.now(), "For test")
      }.also { it.allocations().single().prisonerStatus isEqualTo PrisonerStatus.AUTO_SUSPENDED }

      candidatesSetup(schedule.activity, PrisonerSearchPrisonerFixture.pagedResult(prisonerNumbers = listOf("A1234BC")))
      whenever(allocationRepository.findByAllocationIdIn(any())).thenReturn(schedule.allocations())
      whenever(allocationRepository.getCandidateAllocations(any())).thenReturn(
        schedule.allocations().map {
          TestCandidate(it.allocationId, it.prisonerNumber, it.activitySchedule.activity.activityCategory.code, 99)
        },
      )

      val nonAssociation: NonAssociation = mock {
        on { firstPrisonerNumber } doReturn "A1234BC"
      }

      nonAssociationsApiClient.stub {
        on {
          runBlocking {
            nonAssociationsApiClient.getNonAssociationsInvolving("MDI", listOf("A1234BC"))
          }
        } doReturn listOf(nonAssociation)
      }

      val candidates = service.getActivityCandidates(
        schedule.activityScheduleId,
        null,
        null,
        null,
        null,
        null,
        pageable,
      )

      candidates.content.single().otherAllocations.single() isEqualTo schedule.allocations().single().toModel()
    }

    @Test
    fun `Candidates ACTIVE other allocations are not filtered out`() {
      val schedule = activitySchedule(activityEntity(), noAllocations = true).apply {
        allocatePrisoner(
          prisonerNumber = "A1234BC".toPrisonerNumber(),
          payBand = lowPayBand,
          bookingId = 1L,
          startDate = TimeSource.today(),
          allocatedBy = "Test",
        )
      }.also { it.allocations().single().prisonerStatus isEqualTo PrisonerStatus.ACTIVE }

      candidatesSetup(schedule.activity, PrisonerSearchPrisonerFixture.pagedResult(prisonerNumbers = listOf("A1234BC")))

      whenever(allocationRepository.findByAllocationIdIn(any())).thenReturn(schedule.allocations())
      whenever(allocationRepository.getCandidateAllocations(any())).thenReturn(
        schedule.allocations().map {
          TestCandidate(it.allocationId, it.prisonerNumber, it.activitySchedule.activity.activityCategory.code, 99)
        },
      )

      val candidates = service.getActivityCandidates(
        schedule.activityScheduleId,
        null,
        null,
        null,
        null,
        null,
        pageable,
      )

      candidates.content.single().otherAllocations.single() isEqualTo schedule.allocations().single().toModel()
    }
  }

  @Nested
  inner class NonAssociations {
    @Test
    fun `Should return non-associations`() {
      val schedule = schedule()
      val prisonerNumber = schedule.allocations(true)[0].prisonerNumber
      val prisonerA1234AA = nonAssociation("A1234AA")
      val prisonerB3333BB = nonAssociation("B3333BB")

      whenever(nonAssociationsApiClient.getOffenderNonAssociations(prisonerNumber)).thenReturn(
        listOf(prisonerA1234AA, prisonerB3333BB),
      )

      whenever(activityScheduleRepository.findById(1)).thenReturn(Optional.of(schedule))

      val nonAssociations = service.nonAssociations(1, prisonerNumber)

      assertThat(nonAssociations).isEqualTo(
        listOf(
          NonAssociationDetails(
            allocated = true,
            reasonCode = prisonerA1234AA.reason.toString(),
            reasonDescription = prisonerA1234AA.reasonDescription,
            roleCode = prisonerA1234AA.role.toString(),
            roleDescription = prisonerA1234AA.roleDescription,
            restrictionType = prisonerA1234AA.restrictionType.toString(),
            restrictionTypeDescription = prisonerA1234AA.restrictionTypeDescription,
            otherPrisonerDetails = OtherPrisonerDetailsDto(
              prisonerNumber = prisonerA1234AA.otherPrisonerDetails.prisonerNumber,
              firstName = prisonerA1234AA.otherPrisonerDetails.firstName,
              lastName = prisonerA1234AA.otherPrisonerDetails.lastName,
              cellLocation = prisonerA1234AA.otherPrisonerDetails.cellLocation,
            ),
            whenUpdated = LocalDateTime.parse(prisonerA1234AA.whenUpdated),
            comments = prisonerA1234AA.comment,
          ),
          NonAssociationDetails(
            allocated = false,
            reasonCode = prisonerB3333BB.reason.toString(),
            reasonDescription = prisonerB3333BB.reasonDescription,
            roleCode = prisonerB3333BB.role.toString(),
            roleDescription = prisonerB3333BB.roleDescription,
            restrictionType = prisonerB3333BB.restrictionType.toString(),
            restrictionTypeDescription = prisonerB3333BB.restrictionTypeDescription,
            otherPrisonerDetails = OtherPrisonerDetailsDto(
              prisonerNumber = prisonerB3333BB.otherPrisonerDetails.prisonerNumber,
              firstName = prisonerB3333BB.otherPrisonerDetails.firstName,
              lastName = prisonerB3333BB.otherPrisonerDetails.lastName,
              cellLocation = prisonerB3333BB.otherPrisonerDetails.cellLocation,
            ),
            whenUpdated = LocalDateTime.parse(prisonerB3333BB.whenUpdated),
            comments = prisonerB3333BB.comment,
          ),
        ),
      )
    }
  }

  fun nonAssociation(prisonerNumber: String) = PrisonerNonAssociation(
    id = 1,
    role = PrisonerNonAssociation.Role.VICTIM,
    roleDescription = "Victim",
    reason = PrisonerNonAssociation.Reason.BULLYING,
    reasonDescription = "Bullying",
    restrictionType = PrisonerNonAssociation.RestrictionType.LANDING,
    restrictionTypeDescription = "Landing",
    comment = "Bullying comment",
    authorisedBy = "ADMIN",
    whenCreated = "2022-04-02T11:11:16",
    whenUpdated = "2022-04-14T12:37:16",
    updatedBy = "ADMIN",
    isClosed = false,
    isOpen = true,
    otherPrisonerDetails = OtherPrisonerDetails(
      prisonerNumber = prisonerNumber,
      role = OtherPrisonerDetails.Role.PERPETRATOR,
      roleDescription = "Perpetrator",
      firstName = "Joseph",
      lastName = "Bloggs",
      prisonId = "MDI",
      prisonName = "HMP Moorland",
      cellLocation = "F-2-009",
    ),
  )
}
