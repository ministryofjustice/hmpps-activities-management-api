package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventPriority
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonRegimeDaysOfWeek
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.prisonRegime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonPayBandCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.PrisonPayBandUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.EventPriorityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonPayBandRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.refdata.PrisonRegimeRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.PrisonRegimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.Priority
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.PrisonRegimeService.Companion.getSlotForDayAndTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.time.DayOfWeek
import java.time.LocalTime
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.PrisonPayBand as EntityPrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand as ModelPrisonPayBand

class PrisonRegimeServiceTest {

  private val eventPriorityRepository: EventPriorityRepository = mock()
  private val prisonPayBandRepository: PrisonPayBandRepository = mock()
  private val prisonRegimeRepository: PrisonRegimeRepository = mock()

  private val service = PrisonRegimeService(eventPriorityRepository, prisonPayBandRepository, prisonRegimeRepository)

  private val now = LocalTime.of(9, 0, 0)

  private fun createRegime(daysOfWeek: List<PrisonRegimeDaysOfWeek>): PrisonRegime =
    PrisonRegime(
      prisonCode = "IWI",
      prisonRegimeDaysOfWeek = daysOfWeek,
      amStart = now,
      amFinish = now.plusHours(3),
      pmStart = now.plusHours(4),
      pmFinish = now.plusHours(6),
      edStart = now.plusHours(7),
      edFinish = now.plusHours(9),
    )

  private val iwiRegime = listOf(
    createRegime(
      daysOfWeek = listOf(
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.MONDAY),
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.TUESDAY),
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.WEDNESDAY),
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.THURSDAY),
      ),
    ),
    createRegime(
      daysOfWeek = listOf(
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.FRIDAY),
      ),
    ),
    createRegime(
      daysOfWeek = listOf(
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.SATURDAY),
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.SUNDAY),
      ),
    ),
  )

  private val bciRegime = listOf(
    createRegime(
      daysOfWeek = listOf(
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.MONDAY),
      ),
    ),
    createRegime(
      daysOfWeek = listOf(
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.TUESDAY),
      ),
    ),
    createRegime(
      daysOfWeek = listOf(
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.WEDNESDAY),
      ),
    ),
    createRegime(
      daysOfWeek = listOf(
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.THURSDAY),
      ),
    ),
    createRegime(
      daysOfWeek = listOf(
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.FRIDAY),
      ),
    ),
    createRegime(
      daysOfWeek = listOf(
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.SATURDAY),
      ),
    ),
    createRegime(
      daysOfWeek = listOf(
        PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.SUNDAY),
      ),
    ),
  )

  @BeforeEach
  fun setUp() {
    addCaseloadIdToRequestHeader(MOORLAND_PRISON_CODE)
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

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
  fun `create a new prison pay band`() {
    val request = PrisonPayBandCreateRequest(
      displaySequence = 1,
      nomisPayBand = 1,
      alias = "alias",
      description = "description",
    )

    val moorlandPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 0,
      displaySequence = 1,
      nomisPayBand = 1,
      payBandAlias = "alias",
      payBandDescription = "description",
      prisonCode = "MDI",
    )

    val persistedMoorlandPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 1,
      displaySequence = 1,
      nomisPayBand = 1,
      payBandAlias = "alias",
      payBandDescription = "description",
      prisonCode = "MDI",
    )

    whenever(prisonPayBandRepository.saveAndFlush(moorlandPrisonPayBand)).thenReturn(persistedMoorlandPrisonPayBand)

    val response = service.createPrisonPayBand("MDI", request)

    assertThat(response).isEqualTo(
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
  fun `fails to create a new prison pay band when the nomis pay band exists for the prison`() {
    val moorlandPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 1,
      displaySequence = 1,
      nomisPayBand = 1,
      payBandAlias = "alias",
      payBandDescription = "description",
      prisonCode = "MDI",
    )

    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(listOf(moorlandPrisonPayBand))

    val request = PrisonPayBandCreateRequest(
      displaySequence = 2,
      nomisPayBand = 1,
      alias = "alias",
      description = "description",
    )

    assertThatThrownBy {
      service.createPrisonPayBand("MDI", request)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Nomis pay band 1 already exists in the prison pay band list")
  }

  @Test
  fun `fails to create a new prison pay band when the display sequence exists for the prison`() {
    val moorlandPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 1,
      displaySequence = 1,
      nomisPayBand = 1,
      payBandAlias = "alias",
      payBandDescription = "description",
      prisonCode = "MDI",
    )

    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(listOf(moorlandPrisonPayBand))

    val request = PrisonPayBandCreateRequest(
      displaySequence = 1,
      nomisPayBand = 2,
      alias = "alias",
      description = "description",
    )

    assertThatThrownBy {
      service.createPrisonPayBand("MDI", request)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Display sequence 1 already exists in the prison pay band list")
  }

  @Test
  fun `update an existing prison pay band`() {
    val request = PrisonPayBandUpdateRequest(
      displaySequence = 2,
      nomisPayBand = 2,
      alias = "alias2",
      description = "description2",
    )

    val existingPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 1,
      displaySequence = 1,
      nomisPayBand = 1,
      payBandAlias = "alias",
      payBandDescription = "description",
      prisonCode = "MDI",
    )

    val updatedMoorlandPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 1,
      displaySequence = 2,
      nomisPayBand = 2,
      payBandAlias = "alias2",
      payBandDescription = "description2",
      prisonCode = "MDI",
    )

    whenever(prisonPayBandRepository.findPrisonPayBandByPrisonPayBandIdAndPrisonCode(1, "MDI")).thenReturn(existingPrisonPayBand)
    whenever(prisonPayBandRepository.saveAndFlush(updatedMoorlandPrisonPayBand)).thenReturn(updatedMoorlandPrisonPayBand)

    val response = service.updatePrisonPayBand("MDI", 1, request)

    assertThat(response).isEqualTo(
      ModelPrisonPayBand(
        id = 1,
        displaySequence = 2,
        alias = "alias2",
        description = "description2",
        nomisPayBand = 2,
        prisonCode = "MDI",
      ),
    )
  }

  @Test
  fun `partial update of an existing prison pay band, display sesquence and description`() {
    val request = PrisonPayBandUpdateRequest(
      displaySequence = 2,
      description = "description2",
    )

    val existingPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 1,
      displaySequence = 1,
      nomisPayBand = 1,
      payBandAlias = "alias",
      payBandDescription = "description",
      prisonCode = "MDI",
    )

    val updatedMoorlandPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 1,
      displaySequence = 2,
      nomisPayBand = 1,
      payBandAlias = "alias",
      payBandDescription = "description2",
      prisonCode = "MDI",
    )

    whenever(prisonPayBandRepository.findPrisonPayBandByPrisonPayBandIdAndPrisonCode(1, "MDI")).thenReturn(existingPrisonPayBand)
    whenever(prisonPayBandRepository.saveAndFlush(updatedMoorlandPrisonPayBand)).thenReturn(updatedMoorlandPrisonPayBand)

    val response = service.updatePrisonPayBand("MDI", 1, request)

    assertThat(response).isEqualTo(
      ModelPrisonPayBand(
        id = 1,
        displaySequence = 2,
        nomisPayBand = 1,
        alias = "alias",
        description = "description2",
        prisonCode = "MDI",
      ),
    )
  }

  @Test
  fun `partial update of an existing prison pay band, nomis pay band and alias`() {
    val request = PrisonPayBandUpdateRequest(
      nomisPayBand = 2,
      alias = "alias2",
    )

    val existingPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 1,
      displaySequence = 1,
      nomisPayBand = 1,
      payBandAlias = "alias",
      payBandDescription = "description",
      prisonCode = "MDI",
    )

    val updatedMoorlandPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 1,
      displaySequence = 1,
      nomisPayBand = 2,
      payBandAlias = "alias2",
      payBandDescription = "description",
      prisonCode = "MDI",
    )

    whenever(prisonPayBandRepository.findPrisonPayBandByPrisonPayBandIdAndPrisonCode(1, "MDI")).thenReturn(existingPrisonPayBand)
    whenever(prisonPayBandRepository.saveAndFlush(updatedMoorlandPrisonPayBand)).thenReturn(updatedMoorlandPrisonPayBand)

    val response = service.updatePrisonPayBand("MDI", 1, request)

    assertThat(response).isEqualTo(
      ModelPrisonPayBand(
        id = 1,
        displaySequence = 1,
        nomisPayBand = 2,
        alias = "alias2",
        description = "description",
        prisonCode = "MDI",
      ),
    )
  }

  @Test
  fun `fails to update an existing prison pay band when the nomis pay band exists for the prison`() {
    val request = PrisonPayBandUpdateRequest(
      displaySequence = 2,
      nomisPayBand = 2,
      alias = "alias2",
      description = "description2",
    )

    val existingPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 1,
      displaySequence = 1,
      nomisPayBand = 1,
      payBandAlias = "alias",
      payBandDescription = "description",
      prisonCode = "MDI",
    )

    val otherPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 2,
      displaySequence = 1,
      nomisPayBand = 2,
      payBandAlias = "alias",
      payBandDescription = "description",
      prisonCode = "MDI",
    )

    whenever(prisonPayBandRepository.findPrisonPayBandByPrisonPayBandIdAndPrisonCode(1, "MDI")).thenReturn(existingPrisonPayBand)
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(listOf(existingPrisonPayBand, otherPrisonPayBand))

    assertThatThrownBy {
      service.updatePrisonPayBand("MDI", 1, request)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Nomis pay band 2 already exists in the prison pay band list")
  }

  @Test
  fun `fails to update an existing prison pay band when the display sequence exists for the prison`() {
    val request = PrisonPayBandUpdateRequest(
      displaySequence = 2,
      nomisPayBand = 1,
      alias = "alias2",
      description = "description2",
    )

    val existingPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 1,
      displaySequence = 1,
      nomisPayBand = 1,
      payBandAlias = "alias",
      payBandDescription = "description",
      prisonCode = "MDI",
    )

    val otherPrisonPayBand = EntityPrisonPayBand(
      prisonPayBandId = 2,
      displaySequence = 2,
      nomisPayBand = 2,
      payBandAlias = "alias",
      payBandDescription = "description",
      prisonCode = "MDI",
    )

    whenever(prisonPayBandRepository.findPrisonPayBandByPrisonPayBandIdAndPrisonCode(1, "MDI")).thenReturn(existingPrisonPayBand)
    whenever(prisonPayBandRepository.findByPrisonCode("MDI")).thenReturn(listOf(existingPrisonPayBand, otherPrisonPayBand))

    assertThatThrownBy {
      service.updatePrisonPayBand("MDI", 1, request)
    }.isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("Display sequence 2 already exists in the prison pay band list")
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

    assertThat(service.getPrisonRegimeByPrisonCode("PVI").size).isEqualTo(7)
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
    val prisonTimeSlots = service.getSlotTimesForDaysOfWeek(prisonCode, DayOfWeek.entries.toSet())

    assertThat(prisonTimeSlots!![DayOfWeek.entries.toSet()]!!.values.size).isEqualTo(3)

    assertThat(prisonTimeSlots[DayOfWeek.entries.toSet()]!![TimeSlot.AM]).isEqualTo(LocalTime.of(9, 0) to LocalTime.of(12, 0))
    assertThat(prisonTimeSlots[DayOfWeek.entries.toSet()]!![TimeSlot.PM]).isEqualTo(LocalTime.of(13, 0) to LocalTime.of(16, 30))
    assertThat(prisonTimeSlots[DayOfWeek.entries.toSet()]!![TimeSlot.ED]).isEqualTo(LocalTime.of(18, 0) to LocalTime.of(20, 0))
  }

  @Test
  fun `get prison regime across slots does not match all days`() {
    whenever(prisonRegimeRepository.findByPrisonCode(code = "IWI")).thenReturn(
      listOf(
        createRegime(
          daysOfWeek = listOf(
            PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.MONDAY),
            PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.TUESDAY),
            PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.WEDNESDAY),
            PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.THURSDAY),
          ),
        ),
        createRegime(
          daysOfWeek = listOf(
            PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.SATURDAY),
            PrisonRegimeDaysOfWeek(dayOfWeek = DayOfWeek.SUNDAY),
          ),
        ),

      ),
    )
    val result = service.getSlotTimesForDaysOfWeek(
      prisonCode = "IWI",
      daysOfWeek = DayOfWeek.entries.toSet(),
    )

    assertThat(result).isNull()
  }

  @Test
  fun `get prison regimes across slots matches all days`() {
    whenever(prisonRegimeRepository.findByPrisonCode(code = "IWI")).thenReturn(iwiRegime)

    val result = service.getSlotTimesForDaysOfWeek(
      prisonCode = "IWI",
      daysOfWeek = DayOfWeek.entries.toSet(),
    )

    assertThat(result).isNotNull
    assertThat(result!!.size).isEqualTo(3)
  }

  @Test
  fun `get prison regimes across slots for 2 days in different slots`() {
    whenever(prisonRegimeRepository.findByPrisonCode(code = "IWI")).thenReturn(iwiRegime)

    val result = service.getSlotTimesForDaysOfWeek(
      prisonCode = "IWI",
      daysOfWeek = setOf(DayOfWeek.SATURDAY, DayOfWeek.FRIDAY),
    )

    assertThat(result).isNotNull
    assertThat(result!!.size).isEqualTo(2)
  }

  @Test
  fun `get prison regime for time slot`() {
    whenever(prisonRegimeRepository.findByPrisonCode(code = "IWI")).thenReturn(iwiRegime)

    val result = service.getSlotTimesForTimeSlot(
      prisonCode = "IWI",
      daysOfWeek = setOf(DayOfWeek.SATURDAY),
      timeSlot = TimeSlot.ED,
    )

    assertThat(result).isNotNull
    assertThat(result!!.first).isEqualTo(now.plusHours(7))
    assertThat(result.second).isEqualTo(now.plusHours(9))
  }

  @Test
  fun `get prison regime time for partial days of the week`() {
    whenever(prisonRegimeRepository.findByPrisonCode(code = "BCI")).thenReturn(bciRegime)

    assertThat(
      service.getSlotTimesForTimeSlot(
        prisonCode = "BCI",
        daysOfWeek = setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY),
        timeSlot = TimeSlot.PM,
      ),
    ).isNotNull
  }

  @CsvSource("13, PM", "1, AM", "17, ED", "23, ED", "9, AM", "14, PM")
  @ParameterizedTest
  fun `get prison regime timeslot for a dateTime`(hour: Int, timeSlot: TimeSlot) {
    whenever(prisonRegimeRepository.findByPrisonCode(code = "IWI")).thenReturn(iwiRegime)

    val regime = service.getPrisonRegimesByDaysOfWeek(
      agencyId = "IWI",
    )

    assertThat(
      regime.getSlotForDayAndTime(
        day = DayOfWeek.THURSDAY,
        time = LocalTime.of(hour, 0, 0),
      ),
    ).isEqualTo(timeSlot)
  }

  @Test
  fun `set a regime throws exception if a day of week is missing`() {
    assertThatThrownBy {
      service.setPrisonRegime(
        agencyId = "TST",
        slots = listOf(createRegimeSlot(dayOfWeek = DayOfWeek.MONDAY)),
      )
    }.isInstanceOf(ValidationException::class.java).hasMessage("requires all days of week")
  }

  @Test
  fun `set a prison regime`() {
    Mockito.doNothing().whenever(prisonRegimeRepository).deleteByPrisonCode(any())
    whenever(prisonRegimeRepository.save(any())).thenReturn(
      PrisonRegime(
        prisonCode = "",
        prisonRegimeDaysOfWeek =
        DayOfWeek.entries.map {
          PrisonRegimeDaysOfWeek(dayOfWeek = it)
        },
        amStart = LocalTime.now(),
        amFinish = LocalTime.now(),
        pmStart = LocalTime.now(),
        pmFinish = LocalTime.now(),
        edStart = LocalTime.now(),
        edFinish = LocalTime.now(),
      ),
    )

    val response = service.setPrisonRegime(
      agencyId = "TST",
      slots = DayOfWeek.entries.map {
        createRegimeSlot(dayOfWeek = it)
      },
    )

    assertThat(response.size).isEqualTo(7)
  }

  private fun createRegimeSlot(dayOfWeek: DayOfWeek): PrisonRegimeSlot =
    PrisonRegimeSlot(
      dayOfWeek = dayOfWeek,
      amStart = LocalTime.now(),
      amFinish = LocalTime.now(),
      pmStart = LocalTime.now(),
      pmFinish = LocalTime.now(),
      edStart = LocalTime.now(),
      edFinish = LocalTime.now(),
    )
}
