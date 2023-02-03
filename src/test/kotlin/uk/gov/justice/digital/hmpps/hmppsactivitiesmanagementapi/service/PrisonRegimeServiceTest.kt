package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventPriority
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventPriorityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.PrisonRegimeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonPayBand as EntityPrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand as ModelPrisonPayBand

class PrisonRegimeServiceTest {

  private val eventPriorityRepository: EventPriorityRepository = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()
  private val prisonRegimeRepository: PrisonRegimeRepository = mock()

  private val service = PrisonRegimeService(eventPriorityRepository, prisonPayBandRepository, prisonRegimeRepository)

  @Test
  fun `default priorities are returned when no priorities for prison`() {
    whenever(eventPriorityRepository.findByPrisonCode("PVI")).thenReturn(emptyList())

    assertThat(service.getEventPrioritiesForPrison("PVI")).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        EventType.COURT_HEARING to listOf(Priority(1)),
        EventType.VISIT to listOf(Priority(2)),
        EventType.ADJUDICATION_HEARING to listOf(Priority(3)),
        EventType.APPOINTMENT to listOf(Priority(4)),
        EventType.ACTIVITY to listOf(Priority(5)),
      )
    )

    verify(eventPriorityRepository).findByPrisonCode("PVI")
  }

  @Test
  fun `non-overlapping prison priorities are returned for prison`() {
    whenever(eventPriorityRepository.findByPrisonCode("MDI")).thenReturn(
      listOf(
        priority(EventType.ACTIVITY, 1),
        priority(EventType.APPOINTMENT, 2),
        priority(EventType.VISIT, 3),
        priority(EventType.ADJUDICATION_HEARING, 4),
        priority(EventType.COURT_HEARING, 5),
      )
    )

    assertThat(service.getEventPrioritiesForPrison("MDI")).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        EventType.ACTIVITY to listOf(Priority(1)),
        EventType.APPOINTMENT to listOf(Priority(2)),
        EventType.VISIT to listOf(Priority(3)),
        EventType.ADJUDICATION_HEARING to listOf(Priority(4)),
        EventType.COURT_HEARING to listOf(Priority(5)),
      )
    )

    verify(eventPriorityRepository).findByPrisonCode("MDI")
  }

  @Test
  fun `overlapping prison priorities are returned for prison`() {
    whenever(eventPriorityRepository.findByPrisonCode("MDI")).thenReturn(
      listOf(
        priority(EventType.ACTIVITY, 1),
        priority(EventType.APPOINTMENT, 2),
        priority(EventType.VISIT, 2),
        priority(EventType.ADJUDICATION_HEARING, 3),
        priority(EventType.COURT_HEARING, 4),
      )
    )

    assertThat(service.getEventPrioritiesForPrison("MDI")).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        EventType.ACTIVITY to listOf(Priority(1)),
        EventType.APPOINTMENT to listOf(Priority(2)),
        EventType.VISIT to listOf(Priority(2)),
        EventType.ADJUDICATION_HEARING to listOf(Priority(3)),
        EventType.COURT_HEARING to listOf(Priority(4)),
      )
    )

    verify(eventPriorityRepository).findByPrisonCode("MDI")
  }

  @Test
  fun `non-overlapping prison priorities and categories are returned for prison`() {
    whenever(eventPriorityRepository.findByPrisonCode("MDI")).thenReturn(
      listOf(
        priority(EventType.ACTIVITY, 1).copy(eventCategory = EventCategory.EDUCATION),
        priority(EventType.ACTIVITY, 2).copy(eventCategory = EventCategory.SERVICES),
        priority(EventType.ACTIVITY, 3).copy(eventCategory = EventCategory.GYM_SPORTS_FITNESS),
        priority(EventType.ACTIVITY, 4).copy(eventCategory = EventCategory.INDUCTION),
        priority(EventType.ACTIVITY, 5).copy(eventCategory = EventCategory.INDUSTRIES),
        priority(EventType.ACTIVITY, 6).copy(eventCategory = EventCategory.INTERVENTIONS),
        priority(EventType.APPOINTMENT, 7),
        priority(EventType.ACTIVITY, 8).copy(eventCategory = EventCategory.LEISURE_SOCIAL),
        priority(EventType.VISIT, 9),
        priority(EventType.ADJUDICATION_HEARING, 10),
        priority(EventType.COURT_HEARING, 11),
      )
    )

    assertThat(service.getEventPrioritiesForPrison("MDI")).containsExactlyInAnyOrderEntriesOf(
      mapOf(
        EventType.ACTIVITY to listOf(
          Priority(1, EventCategory.EDUCATION),
          Priority(2, EventCategory.SERVICES),
          Priority(3, EventCategory.GYM_SPORTS_FITNESS),
          Priority(4, EventCategory.INDUCTION),
          Priority(5, EventCategory.INDUSTRIES),
          Priority(6, EventCategory.INTERVENTIONS),
          Priority(8, EventCategory.LEISURE_SOCIAL),
        ),
        EventType.APPOINTMENT to listOf(Priority(7)),
        EventType.VISIT to listOf(Priority(9)),
        EventType.ADJUDICATION_HEARING to listOf(Priority(10)),
        EventType.COURT_HEARING to listOf(Priority(11)),
      )
    )

    verify(eventPriorityRepository).findByPrisonCode("MDI")
  }

  private fun priority(eventType: EventType, priority: Int) =
    EventPriority(prisonCode = "MDI", eventType = eventType, priority = priority)

  @Test
  fun `prison pay bands for Moorland are returned`() {
    val moorlandPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 1,
      displaySequence = 1,
      nomisPayBand = 1,
      payBandAlias = "alias",
      payBandDescription = "description",
      prisonCode = "MDI",
    )

    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(listOf(moorlandPrisonPayBand))

    assertThat(service.getPayBandsForPrison("MDI")).containsExactly(
      ModelPrisonPayBand(
        id = 1,
        displaySequence = 1,
        alias = "alias",
        description = "description",
        nomisPayBand = 1,
        prisonCode = "MDI"
      )
    )
  }

  @Test
  fun `default prison pay bands are returned when no pay bands configured for Moorland`() {
    val defaultPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 1,
      displaySequence = 1,
      nomisPayBand = 1,
      payBandAlias = "alias",
      payBandDescription = "description",
      prisonCode = "DEFAULT",
    )

    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(emptyList())
    whenever(prisonPayBandRepository.findByPrisonCode("DEFAULT")).thenReturn(listOf(defaultPrisonPayBand))

    assertThat(service.getPayBandsForPrison("MDI")).containsExactly(
      ModelPrisonPayBand(
        id = 1,
        displaySequence = 1,
        alias = "alias",
        description = "description",
        nomisPayBand = 1,
        prisonCode = "DEFAULT"
      )
    )
  }

  @Test
  fun `returns a prison regime for known prison code`() {
    whenever(prisonRegimeRepository.findByPrisonCode("PVI")).thenReturn(prisonRegime())

    assertThat(service.getPrisonRegimeByPrisonCode("PVI")).isInstanceOf(PrisonRegime::class.java)
  }

  @Test
  fun `throws entity not found exception for unknown prison code`() {
    Assertions.assertThatThrownBy { service.getPrisonRegimeByPrisonCode("PVX") }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("PVX")
  }
}
