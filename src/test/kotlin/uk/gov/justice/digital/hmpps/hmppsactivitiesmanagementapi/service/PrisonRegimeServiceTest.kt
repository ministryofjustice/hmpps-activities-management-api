package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventPriority
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventPriorityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonRegimeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.Priority
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import java.time.DayOfWeek
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand as EntityPrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand as ModelPrisonPayBand

class PrisonRegimeServiceTest {

  private val eventPriorityRepository: EventPriorityRepository = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()
  private val prisonRegimeRepository: PrisonRegimeRepository = mock()

  private val service = PrisonRegimeService(eventPriorityRepository, prisonPayBandRepository, prisonRegimeRepository)

  @Test
  fun `default priorities are returned when no priorities for prison`() {
    whenever(eventPriorityRepository.findByPrisonCode("PVI")).thenReturn(emptyList())

    with(service.getEventPrioritiesForPrison("PVI")) {
      assertThat(priorities).containsExactlyInAnyOrderEntriesOf(
        mapOf(
          EventType.COURT_HEARING to listOf(Priority(1)),
          EventType.EXTERNAL_TRANSFER to listOf(Priority(2)),
          EventType.VISIT to listOf(Priority(3)),
          EventType.ADJUDICATION_HEARING to listOf(Priority(4)),
          EventType.APPOINTMENT to listOf(Priority(5)),
          EventType.ACTIVITY to listOf(Priority(6)),
        ),
      )

      assertThat(getOrDefault(EventType.COURT_HEARING)).isEqualTo(1)
      assertThat(getOrDefault(EventType.EXTERNAL_TRANSFER)).isEqualTo(2)
      assertThat(getOrDefault(EventType.VISIT)).isEqualTo(3)
      assertThat(getOrDefault(EventType.ADJUDICATION_HEARING)).isEqualTo(4)
      assertThat(getOrDefault(EventType.APPOINTMENT)).isEqualTo(5)
      assertThat(getOrDefault(EventType.ACTIVITY)).isEqualTo(6)
    }

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
        priority(EventType.EXTERNAL_TRANSFER, 6),
      ),
    )

    with(service.getEventPrioritiesForPrison("MDI")) {
      assertThat(priorities).containsExactlyInAnyOrderEntriesOf(
        mapOf(
          EventType.ACTIVITY to listOf(Priority(1)),
          EventType.APPOINTMENT to listOf(Priority(2)),
          EventType.VISIT to listOf(Priority(3)),
          EventType.ADJUDICATION_HEARING to listOf(Priority(4)),
          EventType.COURT_HEARING to listOf(Priority(5)),
          EventType.EXTERNAL_TRANSFER to listOf(Priority(6)),
        ),
      )

      assertThat(getOrDefault(EventType.ACTIVITY)).isEqualTo(1)
      assertThat(getOrDefault(EventType.APPOINTMENT)).isEqualTo(2)
      assertThat(getOrDefault(EventType.VISIT)).isEqualTo(3)
      assertThat(getOrDefault(EventType.ADJUDICATION_HEARING)).isEqualTo(4)
      assertThat(getOrDefault(EventType.COURT_HEARING)).isEqualTo(5)
      assertThat(getOrDefault(EventType.EXTERNAL_TRANSFER)).isEqualTo(6)
    }

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
        priority(EventType.EXTERNAL_TRANSFER, 4),
      ),
    )

    with(service.getEventPrioritiesForPrison("MDI")) {
      assertThat(priorities).containsExactlyInAnyOrderEntriesOf(
        mapOf(
          EventType.ACTIVITY to listOf(Priority(1)),
          EventType.APPOINTMENT to listOf(Priority(2)),
          EventType.VISIT to listOf(Priority(2)),
          EventType.ADJUDICATION_HEARING to listOf(Priority(3)),
          EventType.COURT_HEARING to listOf(Priority(4)),
          EventType.EXTERNAL_TRANSFER to listOf(Priority(4)),
        ),
      )

      assertThat(getOrDefault(EventType.ACTIVITY)).isEqualTo(1)
      assertThat(getOrDefault(EventType.APPOINTMENT)).isEqualTo(2)
      assertThat(getOrDefault(EventType.VISIT)).isEqualTo(2)
      assertThat(getOrDefault(EventType.ADJUDICATION_HEARING)).isEqualTo(3)
      assertThat(getOrDefault(EventType.COURT_HEARING)).isEqualTo(4)
      assertThat(getOrDefault(EventType.EXTERNAL_TRANSFER)).isEqualTo(4)
    }

    verify(eventPriorityRepository).findByPrisonCode("MDI")
  }

  @Test
  fun `non-overlapping prison priorities and categories are returned for prison`() {
    whenever(eventPriorityRepository.findByPrisonCode("MDI")).thenReturn(
      listOf(
        priority(EventType.ACTIVITY, 1).copy(eventCategory = EventCategory.EDUCATION),
        priority(EventType.ACTIVITY, 2).copy(eventCategory = EventCategory.PRISON_JOBS),
        priority(EventType.ACTIVITY, 3).copy(eventCategory = EventCategory.GYM_SPORTS_FITNESS),
        priority(EventType.ACTIVITY, 4).copy(eventCategory = EventCategory.INDUCTION),
        priority(EventType.ACTIVITY, 5).copy(eventCategory = EventCategory.INDUSTRIES),
        priority(EventType.ACTIVITY, 6).copy(eventCategory = EventCategory.INTERVENTIONS),
        priority(EventType.APPOINTMENT, 7),
        priority(EventType.ACTIVITY, 8).copy(eventCategory = EventCategory.OTHER),
        priority(EventType.VISIT, 9),
        priority(EventType.ADJUDICATION_HEARING, 10),
        priority(EventType.COURT_HEARING, 11),
      ),
    )

    with(service.getEventPrioritiesForPrison("MDI")) {
      assertThat(priorities).containsExactlyInAnyOrderEntriesOf(
        mapOf(
          EventType.ACTIVITY to listOf(
            Priority(1, EventCategory.EDUCATION),
            Priority(2, EventCategory.PRISON_JOBS),
            Priority(3, EventCategory.GYM_SPORTS_FITNESS),
            Priority(4, EventCategory.INDUCTION),
            Priority(5, EventCategory.INDUSTRIES),
            Priority(6, EventCategory.INTERVENTIONS),
            Priority(8, EventCategory.OTHER),
          ),
          EventType.APPOINTMENT to listOf(Priority(7)),
          EventType.VISIT to listOf(Priority(9)),
          EventType.ADJUDICATION_HEARING to listOf(Priority(10)),
          EventType.COURT_HEARING to listOf(Priority(11)),
        ),
      )

      assertThat(getOrDefault(EventType.ACTIVITY, uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventCategory.EDUCATION)).isEqualTo(1)
      assertThat(getOrDefault(EventType.ACTIVITY, uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventCategory.PRISON_JOBS)).isEqualTo(2)
      assertThat(getOrDefault(EventType.ACTIVITY, uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventCategory.GYM_SPORTS_FITNESS)).isEqualTo(3)
      assertThat(getOrDefault(EventType.ACTIVITY, uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventCategory.INDUCTION)).isEqualTo(4)
      assertThat(getOrDefault(EventType.ACTIVITY, uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventCategory.INDUSTRIES)).isEqualTo(5)
      assertThat(getOrDefault(EventType.ACTIVITY, uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventCategory.INTERVENTIONS)).isEqualTo(6)
      assertThat(getOrDefault(EventType.APPOINTMENT)).isEqualTo(7)
      assertThat(getOrDefault(EventType.ACTIVITY, uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventCategory.OTHER)).isEqualTo(8)
      assertThat(getOrDefault(EventType.VISIT)).isEqualTo(9)
      assertThat(getOrDefault(EventType.ADJUDICATION_HEARING)).isEqualTo(10)
      assertThat(getOrDefault(EventType.COURT_HEARING)).isEqualTo(11)
    }

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
        prisonCode = "MDI",
      ),
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
        prisonCode = "DEFAULT",
      ),
    )
  }

  @Test
  fun `returns a prison regime for known prison code`() {
    whenever(prisonRegimeRepository.findByPrisonCode("PVI")).thenReturn(listOf(prisonRegime()))

    assertThat(service.getPrisonRegimeByPrisonCode("PVI")).isInstanceOf(PrisonRegime::class.java)
  }

  @Test
  fun `throws entity not found exception for unknown prison code`() {
    assertThatThrownBy { service.getPrisonRegimeByPrisonCode("PVX") }.isInstanceOf(EntityNotFoundException::class.java)
      .hasMessage("PVX")
  }

  @Test
  fun `returns correct times for AM timeslot`() {
    val prisonCode = "PBI"
    whenever(prisonRegimeRepository.findByPrisonCode(prisonCode))
      .thenReturn(listOf(prisonRegime()))
    val result = service.getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.AM, DayOfWeek.MONDAY)!!

    assertThat(result.start).isEqualTo("00:00")
    assertThat(result.end).isEqualTo("13:00")
  }

  @Test
  fun `returns correct times for PM timeslot`() {
    val prisonCode = "PBI"
    whenever(prisonRegimeRepository.findByPrisonCode(prisonCode))
      .thenReturn(listOf(prisonRegime()))
    val result = service.getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.PM, DayOfWeek.MONDAY)!!

    assertThat(result.start).isEqualTo("13:00")
    assertThat(result.end).isEqualTo("18:00")
  }

  @Test
  fun `returns correct times for ED timeslot`() {
    val prisonCode = "PBI"
    whenever(prisonRegimeRepository.findByPrisonCode(prisonCode))
      .thenReturn(listOf(prisonRegime()))
    val result = service.getTimeRangeForPrisonAndTimeSlot(prisonCode, TimeSlot.ED, DayOfWeek.MONDAY)!!

    assertThat(result.start).isEqualTo("18:00")
    assertThat(result.end).isEqualTo("23:59")
  }

  @Test
  fun `returns prison time slot map`() {
    val prisonCode = "PBI"
    whenever(prisonRegimeRepository.findByPrisonCode(prisonCode))
      .thenReturn(listOf(prisonRegime()))
    val prisonTimeSlots = service.getPrisonTimeSlots(prisonCode, DayOfWeek.entries.toSet())

    assertThat(prisonTimeSlots!!.values).hasSize(3)
    assertThat(prisonTimeSlots[TimeSlot.AM]).isEqualTo(LocalTime.of(9, 0) to LocalTime.of(12, 0))
    assertThat(prisonTimeSlots[TimeSlot.PM]).isEqualTo(LocalTime.of(13, 0) to LocalTime.of(16, 30))
    assertThat(prisonTimeSlots[TimeSlot.ED]).isEqualTo(LocalTime.of(18, 0) to LocalTime.of(20, 0))
  }
}
