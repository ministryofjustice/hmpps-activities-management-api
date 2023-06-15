package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonersearchapi.model.PrisonerAlert
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.EducationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.IncentiveLevelSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.NonAssociationSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.ReleaseDateSuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.WRASuitability
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityScheduleRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class CandidatesServiceTest {
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val activityScheduleRepository: ActivityScheduleRepository = mock()
  private val allocationsService: AllocationsService = mock()

  private val service = CandidatesService(
    prisonApiClient,
    prisonerSearchApiClient,
    activityScheduleRepository,
    allocationsService,
  )

  @Nested
  inner class CandidateSuitability {
    private val candidateEducation = Education(
      bookingId = 1,
      studyArea = "English Language",
      educationLevel = "Reading Measure 1.0",
    )

    fun candidateSuitabilitySetup(activity: Activity, candidate: Prisoner) {
      val schedule = activity.schedules().first()

      whenever(activityScheduleRepository.findById(1)).thenReturn(Optional.of(schedule))
      whenever(prisonerSearchApiClient.findByPrisonerNumbers(listOf(candidate.prisonerNumber))).thenReturn(
        Mono.just(listOf(candidate)),
      )
      whenever(prisonApiClient.getEducationLevels(listOf(candidate.prisonerNumber))).thenReturn(
        listOf(candidateEducation),
      )
      whenever(prisonApiClient.getOffenderNonAssociations(candidate.prisonerNumber)).thenReturn(emptyList())
    }

    @Test
    fun `fails if schedule not found`() {
      Assertions.assertThatThrownBy {
        service.candidateSuitability(1, "A1234AA")
      }.isInstanceOf(EntityNotFoundException::class.java)
    }

    @Test
    fun `wraSuitability - suitable`() {
      val activity = activityEntity(
        riskLevel = "high",
      )
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
      val activity = activityEntity(
        riskLevel = "medium",
      )
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
      val activity = activityEntity(
        riskLevel = "medium",
      )
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
      )

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
      val activity = activityEntity(
        riskLevel = "high",
      )
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
      )

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
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
      )

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
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
      )

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
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
      )

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
          earliestReleaseDate = LocalDate.now().plusYears(1),
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
          earliestReleaseDate = LocalDate.now().minusDays(1),
        ),
      )
    }

    @Test
    fun `releaseDate - not suitable (no release date)`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
      )

      candidateSuitabilitySetup(activity, candidate)
      val suitability = service.candidateSuitability(
        activity.schedules().first().activityScheduleId,
        candidate.prisonerNumber,
      )

      assertThat(suitability.releaseDate).isEqualTo(
        ReleaseDateSuitability(
          suitable = false,
          earliestReleaseDate = null,
        ),
      )
    }

    @Test
    fun `nonAssociation - suitable (no non-associations)`() {
      val activity = activityEntity()
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
      )

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
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
      )

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
      val candidate = PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
      )

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
          nonAssociations = listOf(offenderNonAssociation),
        ),
      )
    }
  }
}
