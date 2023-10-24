package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Education
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.OffenderNonAssociationDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.api.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.CurrentIncentive
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.IncentiveLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PagedPrisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerAlert
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toPrisonerNumber
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.waitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCandidate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.EarliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.AllocationPayRate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.EducationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.IncentiveLevelSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.NonAssociationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.ReleaseDateSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.WRASuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.WaitingListRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.transformOffenderNonAssociationDetail
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class CandidatesServiceTest {
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val activityScheduleRepository: ActivityScheduleRepository = mock()
  private val allocationRepository: AllocationRepository = mock()
  private val waitingListRepository: WaitingListRepository = mock()

  private val service = CandidatesService(
    prisonApiClient,
    prisonerSearchApiClient,
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
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf(candidate.prisonerNumber))).thenReturn(
        listOf(candidate),
      )
      whenever(prisonApiClient.getEducationLevels(listOf(candidate.prisonerNumber))).thenReturn(
        listOf(candidateEducation),
      )
      whenever(prisonApiClient.getOffenderNonAssociations(candidate.prisonerNumber)).thenReturn(emptyList())

      whenever(
        allocationRepository.findByPrisonCodeAndPrisonerNumber(
          candidate.prisonId!!,
          candidate.prisonerNumber,
        ),
      ).thenReturn(
        listOf(allocation().copy(allocationId = 1, prisonerNumber = candidate.prisonerNumber)),
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
    fun `nonAssociation - suitable (no non-associations)`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC")

      candidateSuitabilitySetup(activity, candidate)
      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.nonAssociation).isEqualTo(
        NonAssociationSuitability(
          suitable = true,
          nonAssociations = emptyList(),
        ),
      )
    }

    @Test
    fun `nonAssociation - suitable (no conflicting non-associations)`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC")

      candidateSuitabilitySetup(activity, candidate)

      whenever(prisonApiClient.getOffenderNonAssociations(candidate.prisonerNumber)).thenReturn(
        listOf(
          OffenderNonAssociationDetail(
            reasonCode = "VIC",
            reasonDescription = "Victim",
            typeCode = "WING",
            typeDescription = "Do Not Locate on Same Wing",
            effectiveDate = LocalDateTime.now().toIsoDateTime(),
            offenderNonAssociation = OffenderNonAssociation(
              offenderNo = "A1234ZY",
              firstName = "Joseph",
              lastName = "Bloggs",
              reasonCode = "PER",
              reasonDescription = "Perpetrator",
              agencyDescription = "Pentonville (PVI)",
              assignedLivingUnitDescription = "PVI-1-2-4",
              assignedLivingUnitId = 1234,
            ),
          ),
        ),
      )

      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.nonAssociation).isEqualTo(
        NonAssociationSuitability(
          suitable = true,
          nonAssociations = emptyList(),
        ),
      )
    }

    @Test
    fun `nonAssociation - not suitable`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A1234BC")

      candidateSuitabilitySetup(activity, candidate)

      val offenderNonAssociation = OffenderNonAssociationDetail(
        reasonCode = "VIC",
        reasonDescription = "Victim",
        typeCode = "WING",
        typeDescription = "Do Not Locate on Same Wing",
        effectiveDate = LocalDateTime.now().toIsoDateTime(),
        offenderNonAssociation = OffenderNonAssociation(
          offenderNo = "A1234AA",
          firstName = "Joseph",
          lastName = "Bloggs",
          reasonCode = "PER",
          reasonDescription = "Perpetrator",
          agencyDescription = "Pentonville (PVI)",
          assignedLivingUnitDescription = "PVI-1-2-4",
          assignedLivingUnitId = 1234,
        ),
      )

      whenever(prisonApiClient.getOffenderNonAssociations(candidate.prisonerNumber)).thenReturn(
        listOf(offenderNonAssociation),
      )

      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.nonAssociation).isEqualTo(
        NonAssociationSuitability(
          suitable = false,
          nonAssociations = listOf(transformOffenderNonAssociationDetail(offenderNonAssociation)),
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
  }

  @Nested
  inner class GetActivityCandidates {
    private fun candidatesSetup(activity: Activity, candidates: PagedPrisoner, waitingList: List<WaitingList> = emptyList()) {
      addCaseloadIdToRequestHeader("MDI")

      val schedule = activity.schedules().first()

      whenever(activityScheduleRepository.findById(1)).thenReturn(Optional.of(schedule))
      whenever(waitingListRepository.findByActivitySchedule(schedule)).thenReturn(waitingList)
      whenever(prisonerSearchApiClient.getAllPrisonersInPrison(schedule.activity.prisonCode)).thenReturn(Mono.just(candidates))

      whenever(
        allocationRepository.findByPrisonCodeAndPrisonerNumbers(
          schedule.activity.prisonCode,
          candidates.content.map { it.prisonerNumber },
        ),
      ).thenReturn(emptyList())
    }

    @Test
    fun `fetch list of suitable candidates`() {
      val activity = activityEntity()
      val allPrisoners = PrisonerSearchPrisonerFixture.pagedResult(prisonerNumber = "A1234BC")
      val waitingList = listOf(waitingList(prisonCode = activity.prisonCode, prisonerNumber = "A1234BC", initialStatus = WaitingListStatus.REMOVED))

      candidatesSetup(activity, allPrisoners, waitingList)

      val candidates = service.getActivityCandidates(
        activity.schedules().first().activityScheduleId,
        null,
        null,
        null,
        null,
      )

      assertThat(candidates).isEqualTo(
        listOf(
          ActivityCandidate(
            name = "Tim Harrison",
            prisonerNumber = "A1234BC",
            cellLocation = "1-2-3",
            otherAllocations = emptyList(),
            earliestReleaseDate = EarliestReleaseDate(null),
          ),
        ),
      )
    }

    @Test
    fun `Candidates with PENDING waiting list are filtered out`() {
      val activity = activityEntity()
      val allPrisoners = PrisonerSearchPrisonerFixture.pagedResult(prisonerNumber = "A1234BC")
      val waitingList = listOf(waitingList(prisonCode = activity.prisonCode, prisonerNumber = "A1234BC", initialStatus = WaitingListStatus.PENDING))

      candidatesSetup(activity, allPrisoners, waitingList)

      val candidates = service.getActivityCandidates(
        activity.schedules().first().activityScheduleId,
        null,
        null,
        null,
        null,
      )

      assertThat(candidates).isEmpty()
    }

    @Test
    fun `Candidates with APPROVED waiting list are filtered out`() {
      val activity = activityEntity()
      val allPrisoners = PrisonerSearchPrisonerFixture.pagedResult(prisonerNumber = "A1234BC")
      val waitingList = listOf(waitingList(prisonCode = activity.prisonCode, prisonerNumber = "A1234BC", initialStatus = WaitingListStatus.APPROVED))

      candidatesSetup(activity, allPrisoners, waitingList)

      val candidates = service.getActivityCandidates(
        activity.schedules().first().activityScheduleId,
        null,
        null,
        null,
        null,
      )

      assertThat(candidates).isEmpty()
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
        ).deallocateNow()
      }.also { it.allocations().single().prisonerStatus isEqualTo PrisonerStatus.ENDED }

      candidatesSetup(schedule.activity, PrisonerSearchPrisonerFixture.pagedResult(prisonerNumber = "A1234BC"))

      whenever(allocationRepository.findByPrisonCodeAndPrisonerNumbers(schedule.activity.prisonCode, listOf("A1234BC"))) doReturn schedule.allocations()

      val candidates = service.getActivityCandidates(
        schedule.activityScheduleId,
        null,
        null,
        null,
        null,
      )

      candidates.single().otherAllocations.isEmpty() isBool true
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

      candidatesSetup(schedule.activity, PrisonerSearchPrisonerFixture.pagedResult(prisonerNumber = "A1234BC"))

      whenever(allocationRepository.findByPrisonCodeAndPrisonerNumbers(schedule.activity.prisonCode, listOf("A1234BC"))) doReturn schedule.allocations()

      val candidates = service.getActivityCandidates(
        schedule.activityScheduleId,
        null,
        null,
        null,
        null,
      )

      candidates.single().otherAllocations.single() isEqualTo schedule.allocations().single().toModel()
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

      candidatesSetup(schedule.activity, PrisonerSearchPrisonerFixture.pagedResult(prisonerNumber = "A1234BC"))

      whenever(allocationRepository.findByPrisonCodeAndPrisonerNumbers(schedule.activity.prisonCode, listOf("A1234BC"))) doReturn schedule.allocations()

      val candidates = service.getActivityCandidates(
        schedule.activityScheduleId,
        null,
        null,
        null,
        null,
      )

      candidates.single().otherAllocations.single() isEqualTo schedule.allocations().single().toModel()
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

      candidatesSetup(schedule.activity, PrisonerSearchPrisonerFixture.pagedResult(prisonerNumber = "A1234BC"))

      whenever(allocationRepository.findByPrisonCodeAndPrisonerNumbers(schedule.activity.prisonCode, listOf("A1234BC"))) doReturn schedule.allocations()

      val candidates = service.getActivityCandidates(
        schedule.activityScheduleId,
        null,
        null,
        null,
        null,
      )

      candidates.single().otherAllocations.single() isEqualTo schedule.allocations().single().toModel()
    }
  }
}
