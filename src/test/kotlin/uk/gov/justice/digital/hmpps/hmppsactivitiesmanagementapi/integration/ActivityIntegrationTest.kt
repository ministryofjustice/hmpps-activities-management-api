package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import net.javacrumbs.jsonunit.assertj.assertThatJson
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ActivityState
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityPayCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.hasSize
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isBool
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.read
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.educationCategory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testActivityPayRateBand1
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testActivityPayRateBand2
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testActivityPayRateBand3
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandOne
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandThree
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.testdata.testPentonvillePayBandTwo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Activity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleLite
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PayPerSession
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityPayCreateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.ActivityUpdateRequest
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.ActivityRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.CASELOAD_ID
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_ADMIN
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_ACTIVITY_HUB
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.resource.ROLE_PRISON
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundHMPPSDomainEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.PrisonerAllocatedInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ScheduleCreatedInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.BankHolidayService
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*

@TestPropertySource(
  properties = [
    "feature.event.activities.activity-schedule.created=true",
    "feature.event.activities.activity-schedule.amended=true",
    "feature.event.activities.prisoner.allocation-amended=true",
    "feature.audit.service.hmpps.enabled=true",
    "feature.audit.service.local.enabled=true",
  ],
)
class ActivityIntegrationTest : ActivitiesIntegrationTestBase() {
  @Autowired
  private lateinit var activityRepository: ActivityRepository

  @MockitoBean
  private lateinit var bankHolidayService: BankHolidayService

  private val eventCaptor = argumentCaptor<OutboundHMPPSDomainEvent>()

  private val hmppsAuditEventCaptor = argumentCaptor<HmppsAuditEvent>()

  @Autowired
  private lateinit var auditRepository: AuditRepository

  @BeforeEach
  fun setUp() {
    prisonerSearchApiMockServer.stubSearchByPrisonerNumbers(
      listOf("A11111A", "A22222A"),
      listOf(
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A11111A"),
        PrisonerSearchPrisonerFixture.instance(prisonerNumber = "A22222A"),
      ),
    )

    nomisMappingApiMockServer.stubMappingFromNomisId(1)
    nomisMappingApiMockServer.stubMappingFromDpsUuid(UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc"))
  }

  @Test
  fun `createActivity - paid is successful`() {
    prisonApiMockServer.stubGetReferenceCode(
      "EDU_LEVEL",
      "1",
      "prisonapi/education-level-code-1.json",
    )

    prisonApiMockServer.stubGetReferenceCode(
      "STUDY_AREA",
      "ENGLA",
      "prisonapi/study-area-code-ENGLA.json",
    )

    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-id-1.json",
    )

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid()

    val createActivityRequest: ActivityCreateRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-7.json").copy(startDate = TimeSource.tomorrow())

    val activity = webTestClient.createActivity(createActivityRequest)

    with(activity!!) {
      assertThat(id).isNotNull
      assertThat(category.id).isEqualTo(1)
      assertThat(tier!!.id).isEqualTo(2)
      assertThat(organiser!!.id).isEqualTo(1)
      assertThat(eligibilityRules.size).isEqualTo(1)
      assertThat(pay.size).isEqualTo(2)
      assertThat(payChange.size).isEqualTo(0)
      assertThat(createdBy).isEqualTo("test-client")
      assertThat(paid).isTrue()
    }

    val activityPayHistoryList = webTestClient.getActivityPayHistory(1L)
    assertThat(activityPayHistoryList).size().isEqualTo(2)

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.created")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A new activity schedule has been created in the activities management service")
    }

    verify(hmppsAuditApiClient).createEvent(hmppsAuditEventCaptor.capture())
    with(hmppsAuditEventCaptor.firstValue) {
      assertThat(what).isEqualTo("ACTIVITY_CREATED")
      assertThat(who).isEqualTo("test-client")
      assertThatJson(details).isEqualTo(
        """
          {
            activityId: 1,
            activityName: "IT level 1",
            prisonCode: "MDI",
            createdAt: "${'$'}{json-unit.ignore}",
            createdBy: "test-client"
          }
        """.trimIndent(),
      )
    }

    assertThat(auditRepository.findAll().size).isEqualTo(1)
    with(auditRepository.findAll().first()) {
      assertThat(activityId).isEqualTo(1)
      assertThat(username).isEqualTo("test-client")
      assertThat(auditType).isEqualTo(AuditType.ACTIVITY)
      assertThat(detailType).isEqualTo(AuditEventType.ACTIVITY_CREATED)
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(message).startsWith("An activity called 'IT level 1'(1) with category Education and starting on ${TimeSource.tomorrow()} at prison MDI was created")
    }

    with(activityRepository.findLastChangeRevision(1).get().metadata) {
      assertThat(revisionType.name).isEqualTo("INSERT")
      assertThat(revisionInstant.toString()).isNotNull
    }

    with(activityRepository.findRevisions(1).last().entity) {
      assertThat(activityId).isEqualTo(1)
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(summary).isEqualTo("IT level 1")
      assertThat(description).isEqualTo("A basic IT course")
      assertThat(riskLevel).isEqualTo("high")
      assertThat(attendanceRequired).isTrue
      assertThat(activityCategory.activityCategoryId).isEqualTo(1)
      assertThat(activityTier.eventTierId).isEqualTo(2)
      assertThat(paid).isTrue
    }
  }

  @Test
  fun `createActivity - paid with multiple pay rates for a given pay band and incentive level is successful`() {
    prisonApiMockServer.stubGetReferenceCode(
      "EDU_LEVEL",
      "1",
      "prisonapi/education-level-code-1.json",
    )

    prisonApiMockServer.stubGetReferenceCode(
      "STUDY_AREA",
      "ENGLA",
      "prisonapi/study-area-code-ENGLA.json",
    )

    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-id-1.json",
    )

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid()

    val futureDate = LocalDate.now().plusDays(25)
    val apr1 = activityPayCreateRequest(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBandId = 11,
      rate = 125,
    )

    val apr2 = activityPayCreateRequest(
      incentiveNomisCode = "BAS",
      incentiveLevel = "Basic",
      payBandId = 11,
      rate = 150,
      startDate = futureDate,
    )

    val createActivityRequest: ActivityCreateRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-7.json").copy(startDate = TimeSource.tomorrow(), pay = listOf(apr1, apr2))

    val activity = webTestClient.createActivity(createActivityRequest)

    with(activity!!) {
      assertThat(id).isNotNull
      assertThat(category.id).isEqualTo(1)
      assertThat(tier!!.id).isEqualTo(2)
      assertThat(organiser!!.id).isEqualTo(1)
      assertThat(eligibilityRules.size).isEqualTo(1)
      assertThat(pay.size).isEqualTo(2)
      assertThat(payChange.size).isEqualTo(0)
      assertThat(createdBy).isEqualTo("test-client")
      assertThat(paid).isTrue()
    }

    with(activity.pay) {
      this.single { it.startDate == null }
      this.single { it.startDate == futureDate }
    }

    val activityPayHistoryList = webTestClient.getActivityPayHistory(1L)
    assertThat(activityPayHistoryList).size().isEqualTo(2)

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.created")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A new activity schedule has been created in the activities management service")
    }

    verify(hmppsAuditApiClient).createEvent(hmppsAuditEventCaptor.capture())
    with(hmppsAuditEventCaptor.firstValue) {
      assertThat(what).isEqualTo("ACTIVITY_CREATED")
      assertThat(who).isEqualTo("test-client")
      assertThatJson(details).isEqualTo("{\"activityId\":1,\"activityName\":\"IT level 1\",\"prisonCode\":\"MDI\",\"createdAt\":\"\${json-unit.ignore}\",\"createdBy\":\"test-client\"}")
    }

    assertThat(auditRepository.findAll().size).isEqualTo(1)
    with(auditRepository.findAll().first()) {
      assertThat(activityId).isEqualTo(1)
      assertThat(username).isEqualTo("test-client")
      assertThat(auditType).isEqualTo(AuditType.ACTIVITY)
      assertThat(detailType).isEqualTo(AuditEventType.ACTIVITY_CREATED)
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(message).startsWith("An activity called 'IT level 1'(1) with category Education and starting on ${TimeSource.tomorrow()} at prison MDI was created")
    }

    with(activityRepository.findLastChangeRevision(1).get().metadata) {
      assertThat(revisionType.name).isEqualTo("INSERT")
      assertThat(revisionInstant.toString()).isNotNull
    }

    with(activityRepository.findRevisions(1).last().entity) {
      assertThat(activityId).isEqualTo(1)
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(summary).isEqualTo("IT level 1")
      assertThat(activityCategory.activityCategoryId).isEqualTo(1)
      assertThat(activityTier.eventTierId).isEqualTo(2)
      assertThat(paid).isTrue
    }
  }

  @Test
  fun `createActivity - unpaid is successful`() {
    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-id-1.json",
    )

    val location = locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid()

    val newActivity = activityCreateRequest(
      prisonCode = MOORLAND_PRISON_CODE,
      educationLevel = prisonApiMockServer.stubGetReferenceCode("EDU_LEVEL", "1", "prisonapi/education-level-code-1.json"),
      studyArea = prisonApiMockServer.stubGetReferenceCode("STUDY_AREA", "ENGLA", "prisonapi/study-area-code-ENGLA.json"),
      paid = false,
    )

    val activity = webTestClient.createActivity(newActivity)

    with(activity!!) {
      assertThat(id).isNotNull
      assertThat(category.id).isEqualTo(1)
      assertThat(tier!!.id).isEqualTo(2)
      assertThat(organiser!!.id).isEqualTo(1)
      assertThat(eligibilityRules.size).isEqualTo(1)
      assertThat(pay).isEmpty()
      assertThat(createdBy).isEqualTo("test-client")
      assertThat(paid).isFalse()
      assertThat(schedules[0].internalLocation).isEqualTo(InternalLocation(1, location.code, location.localName!!, UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc")))
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.created")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A new activity schedule has been created in the activities management service")
    }

    verify(hmppsAuditApiClient).createEvent(hmppsAuditEventCaptor.capture())
    with(hmppsAuditEventCaptor.firstValue) {
      assertThat(what).isEqualTo("ACTIVITY_CREATED")
      assertThat(who).isEqualTo("test-client")
      assertThatJson(details).isEqualTo("{\"activityId\":1,\"activityName\":\"Test activity\",\"prisonCode\":\"MDI\",\"createdAt\":\"\${json-unit.ignore}\",\"createdBy\":\"test-client\"}")
    }

    assertThat(auditRepository.findAll().size).isEqualTo(1)
    with(auditRepository.findAll().first()) {
      assertThat(activityId).isEqualTo(1)
      assertThat(username).isEqualTo("test-client")
      assertThat(auditType).isEqualTo(AuditType.ACTIVITY)
      assertThat(detailType).isEqualTo(AuditEventType.ACTIVITY_CREATED)
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(message).startsWith("An activity called 'Test activity'(1) with category Education and starting on ${TimeSource.tomorrow()} at prison MDI was created")
    }

    with(activityRepository.findLastChangeRevision(1).get().metadata) {
      assertThat(revisionType.name).isEqualTo("INSERT")
      assertThat(revisionInstant.toString()).isNotNull
    }

    with(activityRepository.findRevisions(1).last().entity) {
      assertThat(activityId).isEqualTo(1)
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(summary).isEqualTo("Test activity")
      assertThat(activityCategory.activityCategoryId).isEqualTo(1)
      assertThat(activityTier.eventTierId).isEqualTo(2)
      assertThat(paid).isFalse()
    }
  }

  @Test
  fun `createActivity - using DPS location UUID is successful`() {
    val location = locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid()

    val newActivity = activityCreateRequest(
      prisonCode = MOORLAND_PRISON_CODE,
      educationLevel = prisonApiMockServer.stubGetReferenceCode("EDU_LEVEL", "1", "prisonapi/education-level-code-1.json"),
      studyArea = prisonApiMockServer.stubGetReferenceCode("STUDY_AREA", "ENGLA", "prisonapi/study-area-code-ENGLA.json"),
      paid = false,
    ).copy(dpsLocationId = UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc"))

    val activity = webTestClient.createActivity(newActivity)

    with(activity!!) {
      assertThat(id).isNotNull
      assertThat(category.id).isEqualTo(1)
      assertThat(tier!!.id).isEqualTo(2)
      assertThat(organiser!!.id).isEqualTo(1)
      assertThat(eligibilityRules.size).isEqualTo(1)
      assertThat(pay).isEmpty()
      assertThat(createdBy).isEqualTo("test-client")
      assertThat(paid).isFalse()
      assertThat(schedules[0].internalLocation).isEqualTo(InternalLocation(1, location.code, location.localName!!, UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc")))
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.created")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A new activity schedule has been created in the activities management service")
    }

    verify(hmppsAuditApiClient).createEvent(hmppsAuditEventCaptor.capture())

    with(hmppsAuditEventCaptor.firstValue) {
      assertThat(what).isEqualTo("ACTIVITY_CREATED")
      assertThat(who).isEqualTo("test-client")
      assertThatJson(details).isEqualTo("{\"activityId\":1,\"activityName\":\"Test activity\",\"prisonCode\":\"MDI\",\"createdAt\":\"\${json-unit.ignore}\",\"createdBy\":\"test-client\"}")
    }

    assertThat(auditRepository.findAll().size).isEqualTo(1)

    with(auditRepository.findAll().first()) {
      assertThat(activityId).isEqualTo(1)
      assertThat(username).isEqualTo("test-client")
      assertThat(auditType).isEqualTo(AuditType.ACTIVITY)
      assertThat(detailType).isEqualTo(AuditEventType.ACTIVITY_CREATED)
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(message).startsWith("An activity called 'Test activity'(1) with category Education and starting on ${TimeSource.tomorrow()} at prison MDI was created")
    }

    with(activityRepository.findLastChangeRevision(1).get().metadata) {
      assertThat(revisionType.name).isEqualTo("INSERT")
      assertThat(revisionInstant.toString()).isNotNull
    }

    with(activityRepository.findRevisions(1).last().entity) {
      assertThat(activityId).isEqualTo(1)
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(summary).isEqualTo("Test activity")
      assertThat(activityCategory.activityCategoryId).isEqualTo(1)
      assertThat(activityTier.eventTierId).isEqualTo(2)
      assertThat(paid).isFalse()
    }
  }

  @Test
  fun `createActivity - create multi-week schedule activity`() {
    prisonApiMockServer.stubGetReferenceCode(
      "EDU_LEVEL",
      "1",
      "prisonapi/education-level-code-1.json",
    )

    prisonApiMockServer.stubGetReferenceCode(
      "STUDY_AREA",
      "ENGLA",
      "prisonapi/study-area-code-ENGLA.json",
    )

    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-id-1.json",
    )

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid()

    val createActivityRequest: ActivityCreateRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-8.json").copy(startDate = TimeSource.tomorrow())

    val activity = webTestClient.createActivity(createActivityRequest)

    with(activity!!) {
      assertThat(id).isNotNull
      assertThat(category.id).isEqualTo(1)
      assertThat(tier!!.id).isEqualTo(1)
      assertThat(pay.size).isEqualTo(2)
      assertThat(payChange.size).isEqualTo(0)
      assertThat(createdBy).isEqualTo("test-client")
      with(schedules.first()) {
        assertThat(scheduleWeeks).isEqualTo(2)
        assertThat(slots.size).isEqualTo(2)
        assertThat(slots.find { it.weekNumber == 1 && it.mondayFlag }).isNotNull
        assertThat(slots.find { it.weekNumber == 1 && it.tuesdayFlag }).isNull()
        assertThat(slots.find { it.weekNumber == 2 && it.tuesdayFlag }).isNotNull
        assertThat(slots.find { it.weekNumber == 2 && it.mondayFlag }).isNull()
      }
    }

    val activityPayHistoryList = webTestClient.getActivityPayHistory(1L)
    assertThat(activityPayHistoryList).size().isEqualTo(2)

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.created")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A new activity schedule has been created in the activities management service")
    }

    verify(hmppsAuditApiClient).createEvent(hmppsAuditEventCaptor.capture())
    with(hmppsAuditEventCaptor.firstValue) {
      assertThat(what).isEqualTo("ACTIVITY_CREATED")
      assertThat(who).isEqualTo("test-client")
      assertThatJson(details).isEqualTo("{\"activityId\":1,\"activityName\":\"IT level 1\",\"prisonCode\":\"MDI\",\"createdAt\":\"\${json-unit.ignore}\",\"createdBy\":\"test-client\"}")
    }

    assertThat(auditRepository.findAll().size).isEqualTo(1)
    with(auditRepository.findAll().first()) {
      assertThat(activityId).isEqualTo(1)
      assertThat(username).isEqualTo("test-client")
      assertThat(auditType).isEqualTo(AuditType.ACTIVITY)
      assertThat(detailType).isEqualTo(AuditEventType.ACTIVITY_CREATED)
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(message).startsWith("An activity called 'IT level 1'(1) with category Education and starting on ${TimeSource.tomorrow()} at prison MDI was created")
    }

    with(activityRepository.findLastChangeRevision(1).get().metadata) {
      assertThat(revisionType.name).isEqualTo("INSERT")
      assertThat(revisionInstant.toString()).isNotNull
    }

    with(activityRepository.findRevisions(1).last().entity) {
      assertThat(activityId).isEqualTo(1)
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(summary).isEqualTo("IT level 1")
      assertThat(activityCategory.activityCategoryId).isEqualTo(1)
      assertThat(activityTier.eventTierId).isEqualTo(1)
      assertThat(paid).isTrue
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `createActivity - should fail to create with existing active activity with same summary`() {
    val activityCreateRequest: ActivityCreateRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-2.json")
      .copy(startDate = TimeSource.tomorrow())

    val error = webTestClient.post()
      .uri("/activities")
      .bodyValue(activityCreateRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(error!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Exception: Change the activity name. There is already an activity called 'Maths'")
      assertThat(developerMessage).isEqualTo("Change the activity name. There is already an activity called 'Maths'")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql("classpath:test_data/seed-duplicate-activity.sql")
  fun `createActivity - should create new activity when existing inactive activity has the same summary`() {
    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-id-1.json",
    )

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid()

    val newActivityWithDuplicateSummary = activityCreateRequest(
      prisonCode = MOORLAND_PRISON_CODE,
      educationLevel = prisonApiMockServer.stubGetReferenceCode("EDU_LEVEL", "1", "prisonapi/education-level-code-1.json"),
      studyArea = prisonApiMockServer.stubGetReferenceCode("STUDY_AREA", "ENGLA", "prisonapi/study-area-code-ENGLA.json"),
    ).copy(summary = "Maths")

    val createdDuplicateActivity = webTestClient.createActivity(newActivityWithDuplicateSummary)

    with(createdDuplicateActivity!!) {
      assertThat(summary.isEqualTo("Maths"))
      assertThat(description.isEqualTo("Test activity"))
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `createActivity - failed tier 1 with non attendance`() {
    val activityCreateRequest: ActivityCreateRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-10.json")
      .copy(startDate = TimeSource.tomorrow())

    val error = webTestClient.post()
      .uri("/activities")
      .bodyValue(activityCreateRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf(ROLE_ACTIVITY_ADMIN)))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(error!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("One or more constraint violations occurred")
      assertThat(developerMessage).isEqualTo("Activity with tier code Tier 1 or Tier 2 must be attended")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  fun `createActivity - failed authorisation`() {
    val activityCreateRequest: ActivityCreateRequest = mapper.read<ActivityCreateRequest>("activity/activity-create-request-2.json").copy(startDate = TimeSource.tomorrow())

    val error = webTestClient.post()
      .uri("/activities")
      .bodyValue(activityCreateRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_NOT_ALLOWED")))
      .exchange()
      .expectStatus().isForbidden
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(error!!) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Access denied: Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get all schedules of an activity`() {
    val schedules = webTestClient.getSchedulesOfAnActivity(1)

    assertThat(schedules).containsExactlyInAnyOrder(
      ActivityScheduleLite(
        id = 1,
        description = "Maths AM",
        internalLocation = InternalLocation(1, "L1", "Location 1"),
        capacity = 10,
        activity = ActivityLite(
          id = 1L,
          attendanceRequired = true,
          inCell = false,
          onWing = false,
          offWing = false,
          pieceWork = false,
          outsideWork = false,
          payPerSession = PayPerSession.H,
          prisonCode = "PVI",
          summary = "Maths",
          description = "Maths Level 1",
          riskLevel = "high",
          minimumEducationLevel = listOf(
            ActivityMinimumEducationLevel(
              id = 1,
              educationLevelCode = "1",
              educationLevelDescription = "Reading Measure 1.0",
              studyAreaCode = "ENGLA",
              studyAreaDescription = "English Language",
            ),
          ),
          category = educationCategory,
          capacity = 20,
          allocated = 5,
          createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
          activityState = ActivityState.LIVE,
          paid = true,
        ),
        slots = listOf(
          ActivityScheduleSlot(
            id = 1L,
            timeSlot = TimeSlot.AM,
            weekNumber = 1,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            daysOfWeek = listOf("Mon"),
            mondayFlag = true,
            tuesdayFlag = false,
            wednesdayFlag = false,
            thursdayFlag = false,
            fridayFlag = false,
            saturdayFlag = false,
            sundayFlag = false,
          ),
        ),
        startDate = LocalDate.of(2022, 10, 10),
        scheduleWeeks = 1,
        usePrisonRegimeTime = true,
      ),
      ActivityScheduleLite(
        id = 2,
        description = "Maths PM",
        internalLocation = InternalLocation(2, "L2", "Location 2"),
        capacity = 10,
        activity = ActivityLite(
          id = 1L,
          prisonCode = "PVI",
          attendanceRequired = true,
          inCell = false,
          onWing = false,
          offWing = false,
          pieceWork = false,
          outsideWork = false,
          payPerSession = PayPerSession.H,
          summary = "Maths",
          description = "Maths Level 1",
          riskLevel = "high",
          minimumEducationLevel = listOf(
            ActivityMinimumEducationLevel(
              id = 1,
              educationLevelCode = "1",
              educationLevelDescription = "Reading Measure 1.0",
              studyAreaCode = "ENGLA",
              studyAreaDescription = "English Language",
            ),
          ),
          category = educationCategory,
          capacity = 20,
          allocated = 5,
          createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
          activityState = ActivityState.LIVE,
          paid = true,
        ),
        slots = listOf(
          ActivityScheduleSlot(
            id = 2L,
            timeSlot = TimeSlot.PM,
            weekNumber = 1,
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(15, 0),
            daysOfWeek = listOf("Mon"),
            mondayFlag = true,
            tuesdayFlag = false,
            wednesdayFlag = false,
            thursdayFlag = false,
            fridayFlag = false,
            saturdayFlag = false,
            sundayFlag = false,
          ),
        ),
        startDate = LocalDate.of(2022, 10, 10),
        scheduleWeeks = 1,
        usePrisonRegimeTime = true,
      ),
    )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-8.sql",
  )
  @Test
  fun `get schedules of an activity with multiple slots`() {
    val schedules = webTestClient.getSchedulesOfAnActivity(1)

    assertThat(schedules).containsExactly(
      ActivityScheduleLite(
        id = 1,
        description = "Maths AM",
        internalLocation = InternalLocation(1, "L1", "Location 1"),
        capacity = 10,
        activity = ActivityLite(
          id = 1L,
          attendanceRequired = true,
          inCell = false,
          onWing = false,
          offWing = false,
          pieceWork = true,
          outsideWork = true,
          payPerSession = PayPerSession.H,
          prisonCode = "PVI",
          summary = "Maths",
          description = "Maths Level 1",
          riskLevel = "high",
          category = educationCategory,
          capacity = 10,
          allocated = 2,
          createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
          activityState = ActivityState.LIVE,
          paid = true,
        ),
        slots = listOf(
          ActivityScheduleSlot(
            id = 1L,
            timeSlot = TimeSlot.AM,
            weekNumber = 1,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            daysOfWeek = listOf("Mon", "Wed"),
            mondayFlag = true,
            tuesdayFlag = false,
            wednesdayFlag = true,
            thursdayFlag = false,
            fridayFlag = false,
            saturdayFlag = false,
            sundayFlag = false,
          ),
          ActivityScheduleSlot(
            id = 2L,
            timeSlot = TimeSlot.PM,
            weekNumber = 1,
            startTime = LocalTime.of(13, 0),
            endTime = LocalTime.of(14, 0),
            daysOfWeek = listOf("Mon", "Thu"),
            mondayFlag = true,
            tuesdayFlag = false,
            wednesdayFlag = false,
            thursdayFlag = true,
            fridayFlag = false,
            saturdayFlag = false,
            sundayFlag = false,
          ),
        ),
        startDate = LocalDate.of(2022, 10, 10),
        scheduleWeeks = 1,
        usePrisonRegimeTime = true,
      ),
    )
  }

  @Sql(
    "classpath:test_data/seed-activity-multi-week-schedule-1.sql",
  )
  @Test
  fun `gets activity schedules for activity with multi-week schedule`() {
    val schedules = webTestClient.getSchedulesOfAnActivity(1)

    assertThat(schedules).containsExactly(
      ActivityScheduleLite(
        id = 1,
        description = "Maths AM",
        internalLocation = InternalLocation(1, "L1", "Location 1"),
        capacity = 10,
        activity = ActivityLite(
          id = 1L,
          attendanceRequired = true,
          inCell = false,
          onWing = false,
          offWing = false,
          pieceWork = true,
          outsideWork = true,
          payPerSession = PayPerSession.H,
          prisonCode = "PVI",
          summary = "Maths",
          description = "Maths Level 1",
          riskLevel = "high",
          category = educationCategory,
          capacity = 10,
          allocated = 2,
          createdTime = LocalDateTime.of(2022, 9, 21, 0, 0, 0),
          activityState = ActivityState.LIVE,
          paid = true,
        ),
        slots = listOf(
          ActivityScheduleSlot(
            id = 1L,
            timeSlot = TimeSlot.AM,
            weekNumber = 1,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            daysOfWeek = listOf("Mon", "Wed"),
            mondayFlag = true,
            tuesdayFlag = false,
            wednesdayFlag = true,
            thursdayFlag = false,
            fridayFlag = false,
            saturdayFlag = false,
            sundayFlag = false,
          ),
          ActivityScheduleSlot(
            id = 2L,
            timeSlot = TimeSlot.PM,
            weekNumber = 1,
            startTime = LocalTime.of(13, 0),
            endTime = LocalTime.of(14, 0),
            daysOfWeek = listOf("Mon", "Thu"),
            mondayFlag = true,
            tuesdayFlag = false,
            wednesdayFlag = false,
            thursdayFlag = true,
            fridayFlag = false,
            saturdayFlag = false,
            sundayFlag = false,
          ),
          ActivityScheduleSlot(
            id = 3L,
            timeSlot = TimeSlot.AM,
            weekNumber = 2,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            daysOfWeek = listOf("Tue", "Fri"),
            mondayFlag = false,
            tuesdayFlag = true,
            wednesdayFlag = false,
            thursdayFlag = false,
            fridayFlag = true,
            saturdayFlag = false,
            sundayFlag = false,
          ),
          ActivityScheduleSlot(
            id = 4L,
            timeSlot = TimeSlot.PM,
            weekNumber = 2,
            startTime = LocalTime.of(13, 0),
            endTime = LocalTime.of(14, 0),
            daysOfWeek = listOf("Mon", "Thu"),
            mondayFlag = true,
            tuesdayFlag = false,
            wednesdayFlag = false,
            thursdayFlag = true,
            fridayFlag = false,
            saturdayFlag = false,
            sundayFlag = false,
          ),
        ),
        startDate = LocalDate.of(2022, 10, 10),
        scheduleWeeks = 2,
        usePrisonRegimeTime = true,
      ),
    )
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `get scheduled maths activities with morning and afternoon`() {
    val mathsLevelOneActivity = with(webTestClient.getActivityById(1)) {
      assertThat(prisonCode).isEqualTo("PVI")
      assertThat(attendanceRequired).isTrue
      assertThat(summary).isEqualTo("Maths")
      assertThat(description).isEqualTo("Maths Level 1")
      assertThat(category).isEqualTo(educationCategory)
      assertThat(tier).isEqualTo(EventTier(1, "TIER_1", "Tier 1"))
      assertThat(pay).isEqualTo(listOf(testActivityPayRateBand1, testActivityPayRateBand2, testActivityPayRateBand3))
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(createdBy).isEqualTo("SEED USER")
      assertThat(createdTime).isEqualTo(LocalDate.of(2022, 9, 21).atStartOfDay())
      assertThat(schedules).hasSize(2)
      this
    }

    val mathsMorning = with(mathsLevelOneActivity.schedule("Maths AM")) {
      assertThat(capacity).isEqualTo(10)
      assertThat(this.slots[0].daysOfWeek).isEqualTo(listOf("Mon"))
      assertThat(allocations).hasSize(3)
      assertThat(internalLocation?.id).isEqualTo(1)
      assertThat(internalLocation?.code).isEqualTo("L1")
      assertThat(internalLocation?.description).isEqualTo("Location 1")
      this
    }

    with(mathsMorning.allocatedPrisoner("A11111A")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandOne)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 9, 0))
    }

    with(mathsMorning.allocatedPrisoner("A22222A")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandTwo)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 10))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 9, 0))
    }

    val mathsAfternoon = with(mathsLevelOneActivity.schedule("Maths PM")) {
      assertThat(capacity).isEqualTo(10)
      assertThat(this.slots[0].daysOfWeek).isEqualTo(listOf("Mon"))
      assertThat(allocations).hasSize(3)
      assertThat(internalLocation?.id).isEqualTo(2)
      assertThat(internalLocation?.code).isEqualTo("L2")
      assertThat(internalLocation?.description).isEqualTo("Location 2")
      this
    }

    with(mathsAfternoon.allocatedPrisoner("A11111A")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandThree)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 12))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 10, 0))
    }

    with(mathsAfternoon.allocatedPrisoner("A22222A")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandThree)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 12))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 10, 10, 0))
    }
  }

  @Sql(
    "classpath:test_data/seed-activity-id-2.sql",
  )
  @Test
  fun `get scheduled english activities for morning and afternoon`() {
    val englishLevelTwoActivity = with(webTestClient.getActivityById(2)) {
      assertThat(attendanceRequired).isTrue
      assertThat(summary).isEqualTo("English")
      assertThat(description).isEqualTo("English Level 2")
      assertThat(category).isEqualTo(educationCategory)
      assertThat(tier).isEqualTo(EventTier(1, "TIER_1", "Tier 1"))
      assertThat(pay).isEqualTo(listOf(testActivityPayRateBand1, testActivityPayRateBand2, testActivityPayRateBand3))
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(createdBy).isEqualTo("SEED USER")
      assertThat(createdTime).isEqualTo(LocalDate.of(2022, 9, 21).atStartOfDay())
      assertThat(schedules).hasSize(2)
      this
    }

    val englishMorning = with(englishLevelTwoActivity.schedule("English AM")) {
      assertThat(capacity).isEqualTo(10)
      assertThat(this.slots[0].daysOfWeek).isEqualTo(listOf("Mon"))
      assertThat(allocations).hasSize(2)
      assertThat(internalLocation?.id).isEqualTo(3)
      assertThat(internalLocation?.code).isEqualTo("L3")
      assertThat(internalLocation?.description).isEqualTo("Location 3")
      this
    }

    with(englishMorning.allocatedPrisoner("B11111B")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandOne)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }

    with(englishMorning.allocatedPrisoner("B22222B")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandTwo)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }

    val englishAfternoon = with(englishLevelTwoActivity.schedule("English PM")) {
      assertThat(description).isEqualTo("English PM")
      assertThat(capacity).isEqualTo(10)
      assertThat(this.slots[0].daysOfWeek).isEqualTo(listOf("Mon"))
      assertThat(allocations).hasSize(2)
      assertThat(internalLocation?.id).isEqualTo(4)
      assertThat(internalLocation?.code).isEqualTo("L4")
      assertThat(internalLocation?.description).isEqualTo("Location 4")
      this
    }

    with(englishAfternoon.allocatedPrisoner("B11111B")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandThree)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MR BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }

    with(englishAfternoon.allocatedPrisoner("B22222B")) {
      assertThat(prisonPayBand).isEqualTo(testPentonvillePayBandThree)
      assertThat(startDate).isEqualTo(LocalDate.of(2022, 10, 21))
      assertThat(endDate).isNull()
      assertThat(allocatedBy).isEqualTo("MRS BLOGS")
      assertThat(allocatedTime).isEqualTo(LocalDateTime.of(2022, 10, 21, 0, 0))
    }
  }

  private fun WebTestClient.getSchedulesOfAnActivity(id: Long) = get()
    .uri("/activities/$id/schedules")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf(ROLE_PRISON)))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ActivityScheduleLite::class.java)
    .returnResult().responseBody

  private fun WebTestClient.createActivity(
    activityCreateRequest: ActivityCreateRequest,
  ) = post()
    .uri("/activities")
    .bodyValue(activityCreateRequest)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
    .header(CASELOAD_ID, "MDI")
    .exchange()
    .expectStatus().isCreated
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Activity::class.java)
    .returnResult().responseBody

  private fun WebTestClient.updateActivity(
    prisonCode: String,
    id: Long,
    activityUpdateRequest: ActivityUpdateRequest,
  ) = patch()
    .uri("/activities/$prisonCode/activityId/$id")
    .bodyValue(activityUpdateRequest)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(isClientToken = false, roles = listOf(ROLE_ACTIVITY_HUB)))
    .header(CASELOAD_ID, prisonCode)
    .exchange()
    .expectStatus().isAccepted
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Activity::class.java)
    .returnResult().responseBody!!

  private fun Activity.schedule(description: String) = schedules.schedule(description)

  private fun List<ActivitySchedule>.schedule(description: String) = firstOrNull { it.description.uppercase() == description.uppercase() }
    ?: throw RuntimeException("Activity schedule $description not found.")

  @Test
  @Sql("classpath:test_data/seed-activity-id-19.sql")
  fun `updateActivity - is successful`() {
    prisonApiMockServer.stubGetReferenceCode(
      "EDU_LEVEL",
      "1",
      "prisonapi/education-level-code-1.json",
    )

    prisonApiMockServer.stubGetReferenceCode(
      "STUDY_AREA",
      "ENGLA",
      "prisonapi/study-area-code-ENGLA.json",
    )

    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-PVI.json",
    )

    val updateActivityRequest: ActivityUpdateRequest = mapper.read("activity/activity-update-request-1.json")

    val activity = webTestClient.updateActivity(PENTONVILLE_PRISON_CODE, 1, updateActivityRequest)

    with(activity) {
      assertThat(id).isNotNull
      assertThat(category.id).isEqualTo(1)
      assertThat(summary).isEqualTo("IT level 1 - updated")
      assertThat(tier!!.id).isEqualTo(2)
      assertThat(organiser!!.id).isEqualTo(1)
      assertThat(pay.size).isEqualTo(1)
      assertThat(payChange.size).isEqualTo(0)
      assertThat(updatedBy).isEqualTo("test-client")
      assertThat(schedules.first().updatedBy).isEqualTo("test-client")
    }

    val activityPayHistoryList = webTestClient.getActivityPayHistory(1L)
    assertThat(activityPayHistoryList).size().isEqualTo(1)

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.amended")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An activity schedule has been updated in the activities management service")
    }

    verify(hmppsAuditApiClient).createEvent(hmppsAuditEventCaptor.capture())
    with(hmppsAuditEventCaptor.firstValue) {
      assertThat(what).isEqualTo("ACTIVITY_UPDATED")
      assertThat(who).isEqualTo("test-client")
      assertThatJson(details).isEqualTo("{\"activityId\":1,\"activityName\":\"IT level 1 - updated\",\"prisonCode\":\"PVI\",\"createdAt\":\"\${json-unit.ignore}\",\"createdBy\":\"test-client\"}")
    }

    assertThat(auditRepository.findAll().size).isEqualTo(1)
    with(auditRepository.findAll().first()) {
      assertThat(activityId).isEqualTo(1)
      assertThat(username).isEqualTo("test-client")
      assertThat(auditType).isEqualTo(AuditType.ACTIVITY)
      assertThat(detailType).isEqualTo(AuditEventType.ACTIVITY_UPDATED)
      assertThat(prisonCode).isEqualTo(PENTONVILLE_PRISON_CODE)
      assertThat(message).startsWith("An activity called 'IT level 1 - updated'(1) with category Education and starting on 2022-10-10 at prison PVI was updated")
    }

    with(activityRepository.findLastChangeRevision(1).get().metadata) {
      assertThat(revisionType.name).isEqualTo("UPDATE")
      assertThat(revisionInstant.toString()).isNotNull
    }

    with(activityRepository.findRevisions(1).last().entity) {
      assertThat(activityId).isEqualTo(1)
      assertThat(prisonCode).isEqualTo("PVI")
      assertThat(summary).isEqualTo("IT level 1 - updated")
      assertThat(activityCategory.activityCategoryId).isEqualTo(1)
      assertThat(activityTier.eventTierId).isEqualTo(2)
      assertThat(paid).isTrue
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-19.sql")
  fun `updateActivity pay - is successful`() {
    val newPay = ActivityUpdateRequest(
      pay = listOf(
        ActivityPayCreateRequest(
          incentiveNomisCode = "BAS",
          incentiveLevel = "Basic",
          payBandId = 3,
          rate = 100,
        ),
      ),
    )

    with(webTestClient.updateActivity(PENTONVILLE_PRISON_CODE, 1, newPay).schedules.first()) {
      assertThat(allocations).hasSize(3)
      assertThat(allocations.filter { a -> a.prisonPayBand?.id == 3L }).hasSize(2)
      assertThat(allocations.filter { a -> a.prisonPayBand?.id == 2L }).hasSize(1)
    }

    verify(eventsPublisher, times(3)).send(eventCaptor.capture())

    val allEvents = eventCaptor.allValues
    assertThat(allEvents.size).isEqualTo(3)
    val scheduleUpdateEvent = allEvents.first { it.additionalInformation == ScheduleCreatedInformation(1) }
    val allocation1AmendEvent = allEvents.first { it.additionalInformation == PrisonerAllocatedInformation(1) }
    val allocation12mendEvent = allEvents.first { it.additionalInformation == PrisonerAllocatedInformation(2) }

    with(scheduleUpdateEvent) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.amended")
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An activity schedule has been updated in the activities management service")
    }
    with(allocation1AmendEvent) {
      assertThat(eventType).isEqualTo("activities.prisoner.allocation-amended")
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A prisoner allocation has been amended in the activities management service")
    }
    with(allocation12mendEvent) {
      assertThat(eventType).isEqualTo("activities.prisoner.allocation-amended")
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A prisoner allocation has been amended in the activities management service")
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-19.sql")
  fun `updateActivity pay - pay band with start date - is successful`() {
    val newPay = ActivityUpdateRequest(
      paid = true,
      attendanceRequired = true,
      pay = listOf(
        ActivityPayCreateRequest(
          incentiveNomisCode = "BAS",
          incentiveLevel = "Basic",
          payBandId = 1,
          rate = 67,
          startDate = LocalDate.now().plusDays(2),
        ),
      ),
    )

    with(webTestClient.updateActivity(PENTONVILLE_PRISON_CODE, 1, newPay)) {
      assertThat(pay).hasSize(1)

      with(pay) {
        this.single { it.startDate == LocalDate.now().plusDays(2) }
      }

      with(schedules.first()) {
        assertThat(allocations).hasSize(3)
        assertThat(allocations.filter { a -> a.prisonPayBand?.id == 2L }).hasSize(2)
        assertThat(allocations.filter { a -> a.prisonPayBand?.id == 1L }).hasSize(1)
      }
    }

    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.amended")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An activity schedule has been updated in the activities management service")
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-19.sql")
  fun `updateActivity to in-cell - is successful`() {
    with(webTestClient.updateActivity(PENTONVILLE_PRISON_CODE, 1, ActivityUpdateRequest(inCell = true))) {
      inCell isEqualTo true
      assertThat(schedules.first().internalLocation).isNull()
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-19.sql")
  fun `updateActivity to on-wing and back to internal DPS location`() {
    prisonApiMockServer.stubGetLocation(1L, "prisonapi/location-PVI.json")

    val location = dpsLocation(prisonId = PENTONVILLE_PRISON_CODE)

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(location = location)

    with(webTestClient.updateActivity(PENTONVILLE_PRISON_CODE, 1, ActivityUpdateRequest(onWing = true))) {
      onWing isEqualTo true
      assertThat(schedules.first().internalLocation).isNull()
    }

    with(webTestClient.updateActivity(PENTONVILLE_PRISON_CODE, 1, ActivityUpdateRequest(dpsLocationId = UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc")))) {
      onWing isEqualTo false
      with(schedules.first()) {
        internalLocation?.id isEqualTo 1
        internalLocation?.dpsLocationId isEqualTo UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc")
        internalLocation?.code isEqualTo location.code
        internalLocation?.description isEqualTo location.localName
      }
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-update-slot.sql")
  fun `updateActivity slot and instances - is successful`() {
    with(webTestClient.getActivityById(1).schedules.first()) {
      assertThat(slots).hasSize(1)
      assertThat(slots.first().daysOfWeek).containsExactly("Mon")
      assertThat(instances).isEmpty()
    }

    val mondayTuesdaySlot = ActivityUpdateRequest(
      slots = listOf(
        Slot(
          weekNumber = 1,
          timeSlot = TimeSlot.AM,
          monday = true,
          tuesday = true,
        ),
      ),
    )

    with(webTestClient.updateActivity(MOORLAND_PRISON_CODE, 1, mondayTuesdaySlot).schedules.first()) {
      assertThat(slots).hasSize(1)
      assertThat(slots.first().daysOfWeek).containsExactly("Mon", "Tue")
      assertThat(instances).hasSizeBetween(3, 4)
    }

    val thursdaySlot = ActivityUpdateRequest(
      slots = listOf(
        Slot(
          weekNumber = 1,
          timeSlot = TimeSlot.AM,
          thursday = true,
        ),
      ),
    )

    with(webTestClient.updateActivity(MOORLAND_PRISON_CODE, 1, thursdaySlot).schedules.first()) {
      assertThat(slots).hasSize(1)
      assertThat(slots.first().daysOfWeek).containsExactly("Thu")
      assertThat(instances).hasSizeBetween(1, 2)
    }

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.amended")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An activity schedule has been updated in the activities management service")
    }

    with(eventCaptor.secondValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.amended")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An activity schedule has been updated in the activities management service")
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-with-active-exclusions.sql")
  fun `updateActivity slots with exclusions - is successful`() {
    val mondayTuesdaySlot = ActivityUpdateRequest(
      slots = listOf(
        Slot(
          weekNumber = 1,
          timeSlot = TimeSlot.AM,
          monday = true,
          tuesday = true,
        ),
        Slot(
          weekNumber = 1,
          timeSlot = TimeSlot.PM,
          monday = true,
          tuesday = true,
        ),
      ),
    )

    with(webTestClient.updateActivity(MOORLAND_PRISON_CODE, 1, mondayTuesdaySlot).schedules.first()) {
      assertThat(scheduleWeeks).isEqualTo(1)
      assertThat(slots).hasSize(2)
      assertThat(slots[0].daysOfWeek).containsExactly("Mon", "Tue")
      assertThat(slots[1].daysOfWeek).containsExactly("Mon", "Tue")
    }

    verify(eventsPublisher, times(2)).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      assertThat(eventType).isEqualTo("activities.activity-schedule.amended")
      assertThat(additionalInformation).isEqualTo(ScheduleCreatedInformation(1))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("An activity schedule has been updated in the activities management service")
    }

    with(eventCaptor.secondValue) {
      assertThat(eventType).isEqualTo("activities.prisoner.allocation-amended")
      assertThat(additionalInformation).isEqualTo(PrisonerAllocatedInformation(2))
      assertThat(occurredAt).isCloseTo(LocalDateTime.now(), within(60, ChronoUnit.SECONDS))
      assertThat(description).isEqualTo("A prisoner allocation has been amended in the activities management service")
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-update-start-and-end-date.sql")
  fun `updateActivity start date - is successful`() {
    val tomorrow = LocalDate.now().plusDays(1)

    with(webTestClient.getActivityById(1)) {
      assertThat(startDate).isEqualTo(tomorrow)
      assertThat(schedules).hasSize(1)
      assertThat(schedules.first().startDate).isEqualTo(tomorrow)
      assertThat(schedules.first().allocations).hasSize(1)
      assertThat(schedules.first().allocations.first().startDate).isEqualTo(tomorrow.plusDays(2))
      assertThat(schedules.first().instances.first().advanceAttendances).hasSize(1)
    }

    val newActivityStartDate = LocalDate.now().plusDays(3)

    val newEndDate = ActivityUpdateRequest(startDate = newActivityStartDate)

    with(webTestClient.updateActivity(PENTONVILLE_PRISON_CODE, 1, newEndDate)) {
      assertThat(startDate).isEqualTo(newActivityStartDate)
      assertThat(schedules.first().startDate).isEqualTo(newActivityStartDate)
      assertThat(schedules.first().allocations.first().startDate).isEqualTo(newActivityStartDate)
      assertThat(schedules.first().instances).isEmpty()
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-update-start-and-end-date.sql")
  fun `updateActivity end date - is successful`() {
    with(webTestClient.getActivityById(1)) {
      assertThat(endDate).isNull()
      assertThat(schedules).hasSize(1)
      assertThat(schedules.first().endDate).isNull()
      assertThat(schedules.first().allocations).hasSize(1)
      assertThat(schedules.first().allocations.first().endDate).isNotNull()
      assertThat(schedules.first().instances.first().advanceAttendances).hasSize(1)
    }

    val dayAfterTomorrow = LocalDate.now().plusDays(3)

    val newEndDate = ActivityUpdateRequest(endDate = dayAfterTomorrow)

    with(webTestClient.updateActivity(PENTONVILLE_PRISON_CODE, 1, newEndDate)) {
      assertThat(endDate).isEqualTo(dayAfterTomorrow)
      assertThat(schedules.first().endDate).isEqualTo(dayAfterTomorrow)
      assertThat(schedules.first().allocations.first().endDate).isEqualTo(dayAfterTomorrow)
      assertThat(schedules.first().instances).isEmpty()
    }
  }

  @Test
  fun `updateActivity - runs on bank holidays updated to not run on bank holidays`() {
    prisonApiMockServer.stubGetReferenceCode(
      "EDU_LEVEL",
      "1",
      "prisonapi/education-level-code-1.json",
    )

    prisonApiMockServer.stubGetReferenceCode(
      "STUDY_AREA",
      "ENGLA",
      "prisonapi/study-area-code-ENGLA.json",
    )

    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-id-1.json",
    )

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid()

    val startDate = TimeSource.tomorrow()

    whenever(bankHolidayService.isEnglishBankHoliday(startDate)) doReturn true

    val createActivityRequest: ActivityCreateRequest =
      mapper.read<ActivityCreateRequest>("activity/activity-create-request-7.json")
        .copy(
          startDate = startDate,
          slots = listOf(
            Slot(
              weekNumber = 1,
              timeSlot = TimeSlot.AM,
              monday = startDate.dayOfWeek == DayOfWeek.MONDAY,
              tuesday = startDate.dayOfWeek == DayOfWeek.TUESDAY,
              wednesday = startDate.dayOfWeek == DayOfWeek.WEDNESDAY,
              thursday = startDate.dayOfWeek == DayOfWeek.THURSDAY,
              friday = startDate.dayOfWeek == DayOfWeek.FRIDAY,
              saturday = startDate.dayOfWeek == DayOfWeek.SATURDAY,
              sunday = startDate.dayOfWeek == DayOfWeek.SUNDAY,
            ),
          ),
          runsOnBankHoliday = true,
        )

    val activity = webTestClient.createActivity(createActivityRequest)!!

    activity.schedules.flatMap { it.instances } hasSize 2

    val updatedActivity = webTestClient.updateActivity(activity.prisonCode, activity.id, ActivityUpdateRequest(runsOnBankHoliday = false))

    updatedActivity.schedules.flatMap { it.instances } hasSize 1
  }

  @Test
  fun `updateActivity - does not run on bank holidays changed to does run on bank holidays`() {
    prisonApiMockServer.stubGetReferenceCode(
      "EDU_LEVEL",
      "1",
      "prisonapi/education-level-code-1.json",
    )

    prisonApiMockServer.stubGetReferenceCode(
      "STUDY_AREA",
      "ENGLA",
      "prisonapi/study-area-code-ENGLA.json",
    )

    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-id-1.json",
    )

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid()

    val startDate = TimeSource.tomorrow()

    whenever(bankHolidayService.isEnglishBankHoliday(startDate)) doReturn true

    val createActivityRequest: ActivityCreateRequest =
      mapper.read<ActivityCreateRequest>("activity/activity-create-request-7.json")
        .copy(
          startDate = startDate,
          slots = listOf(
            Slot(
              weekNumber = 1,
              timeSlot = TimeSlot.AM,
              monday = startDate.dayOfWeek == DayOfWeek.MONDAY,
              tuesday = startDate.dayOfWeek == DayOfWeek.TUESDAY,
              wednesday = startDate.dayOfWeek == DayOfWeek.WEDNESDAY,
              thursday = startDate.dayOfWeek == DayOfWeek.THURSDAY,
              friday = startDate.dayOfWeek == DayOfWeek.FRIDAY,
              saturday = startDate.dayOfWeek == DayOfWeek.SATURDAY,
              sunday = startDate.dayOfWeek == DayOfWeek.SUNDAY,
            ),
          ),
          runsOnBankHoliday = false,
        )

    val activity = webTestClient.createActivity(createActivityRequest)!!

    activity.schedules.flatMap { it.instances } hasSize 1

    val updatedActivity = webTestClient.updateActivity(activity.prisonCode, activity.id, ActivityUpdateRequest(runsOnBankHoliday = true))

    updatedActivity.schedules.flatMap { it.instances } hasSize 2
  }

  @Test
  fun `updateActivity - adds second week slots to activity with multi-week schedule`() {
    prisonApiMockServer.stubGetReferenceCode(
      "EDU_LEVEL",
      "1",
      "prisonapi/education-level-code-1.json",
    )

    prisonApiMockServer.stubGetReferenceCode(
      "STUDY_AREA",
      "ENGLA",
      "prisonapi/study-area-code-ENGLA.json",
    )

    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-id-1.json",
    )

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid()

    val startDate = TimeSource.tomorrow()

    val weekOneSlot = Slot(
      weekNumber = 1,
      timeSlot = TimeSlot.AM,
      monday = startDate.dayOfWeek == DayOfWeek.MONDAY,
      tuesday = startDate.dayOfWeek == DayOfWeek.TUESDAY,
      wednesday = startDate.dayOfWeek == DayOfWeek.WEDNESDAY,
      thursday = startDate.dayOfWeek == DayOfWeek.THURSDAY,
      friday = startDate.dayOfWeek == DayOfWeek.FRIDAY,
      saturday = startDate.dayOfWeek == DayOfWeek.SATURDAY,
      sunday = startDate.dayOfWeek == DayOfWeek.SUNDAY,
    )

    val weekTwoSlot = Slot(
      weekNumber = 2,
      timeSlot = TimeSlot.AM,
      monday = startDate.plusDays(1).dayOfWeek == DayOfWeek.MONDAY,
      tuesday = startDate.plusDays(1).dayOfWeek == DayOfWeek.TUESDAY,
      wednesday = startDate.plusDays(1).dayOfWeek == DayOfWeek.WEDNESDAY,
      thursday = startDate.plusDays(1).dayOfWeek == DayOfWeek.THURSDAY,
      friday = startDate.plusDays(1).dayOfWeek == DayOfWeek.FRIDAY,
      saturday = startDate.plusDays(1).dayOfWeek == DayOfWeek.SATURDAY,
      sunday = startDate.plusDays(1).dayOfWeek == DayOfWeek.SUNDAY,
    )

    val createActivityRequest: ActivityCreateRequest =
      mapper.read<ActivityCreateRequest>("activity/activity-create-request-7.json").copy(
        startDate = startDate,
        scheduleWeeks = 2,
        slots = listOf(weekOneSlot),
      )

    val activity = webTestClient.createActivity(createActivityRequest)!!

    activity.schedules.flatMap { it.instances } hasSize 1

    val updatedActivity = webTestClient.updateActivity(
      activity.prisonCode,
      activity.id,
      ActivityUpdateRequest(
        slots = listOf(weekOneSlot, weekTwoSlot),
      ),
    )

    updatedActivity.schedules.flatMap { it.instances } hasSize 2
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-19.sql")
  fun `updateActivity - Add organiser`() {
    val activity = webTestClient.getActivityById(1, "PVI")
    activity.organiser isEqualTo EventOrganiser(
      id = 1,
      code = "PRISON_STAFF",
      description = "Prison staff",
    )

    // Add organiser
    val updatedActivity = webTestClient.updateActivity(
      "PVI",
      1,
      ActivityUpdateRequest(tierCode = "TIER_2", organiserCode = "PRISONER"),
    )
    updatedActivity.organiser isEqualTo EventOrganiser(
      id = 2,
      code = "PRISONER",
      description = "A prisoner or group of prisoners",
    )
  }

  @Test
  @Sql("classpath:test_data/seed-activity-paid-no-allocations.sql")
  fun `updateActivity - can change paid activity to unpaid activity`() {
    with(webTestClient.getActivityById(1, "PVI")) {
      paid isBool true
      pay.isNotEmpty() isBool true
    }

    with(webTestClient.updateActivity("PVI", 1, ActivityUpdateRequest(paid = false))) {
      paid isBool false
      pay hasSize 0
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-unpaid-no-allocations-or-pay-rates.sql")
  fun `updateActivity - can change unpaid activity to paid activity`() {
    with(webTestClient.getActivityById(1, "PVI")) {
      paid isBool false
      pay hasSize 0
    }

    val activityPay = ActivityPayCreateRequest(incentiveNomisCode = "BAS", incentiveLevel = "Basic", payBandId = 1L, rate = 125, pieceRate = 150, pieceRateItems = 1)

    with(webTestClient.updateActivity("PVI", 1, ActivityUpdateRequest(paid = true, pay = listOf(activityPay)))) {
      paid isBool true
      pay.isNotEmpty() isBool true
    }
  }

  @Test
  fun `updateActivity - change only the timeslot of a custom times schedule`() {
    prisonApiMockServer.stubGetReferenceCode(
      "EDU_LEVEL",
      "1",
      "prisonapi/education-level-code-1.json",
    )

    prisonApiMockServer.stubGetReferenceCode(
      "STUDY_AREA",
      "ENGLA",
      "prisonapi/study-area-code-ENGLA.json",
    )

    prisonApiMockServer.stubGetLocation(
      1L,
      "prisonapi/location-id-1.json",
    )

    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid()

    val startDate = TimeSource.tomorrow()

    val pmSlot = Slot(
      weekNumber = 1,
      timeSlot = TimeSlot.PM,
      monday = startDate.dayOfWeek == DayOfWeek.MONDAY,
      tuesday = startDate.dayOfWeek == DayOfWeek.TUESDAY,
      wednesday = startDate.dayOfWeek == DayOfWeek.WEDNESDAY,
      thursday = startDate.dayOfWeek == DayOfWeek.THURSDAY,
      friday = startDate.dayOfWeek == DayOfWeek.FRIDAY,
      saturday = startDate.dayOfWeek == DayOfWeek.SATURDAY,
      sunday = startDate.dayOfWeek == DayOfWeek.SUNDAY,
      customStartTime = LocalTime.of(9, 45),
      customEndTime = LocalTime.of(11, 45),
    )

    val createActivityRequest: ActivityCreateRequest =
      mapper.read<ActivityCreateRequest>("activity/activity-create-request-7.json").copy(
        startDate = startDate,
        scheduleWeeks = 1,
        slots = listOf(pmSlot),
      )

    val activity = webTestClient.createActivity(createActivityRequest)!!

    activity.schedules.flatMap { it.instances } hasSize 2

    val amSlot = Slot(
      weekNumber = 1,
      timeSlot = TimeSlot.AM,
      monday = startDate.dayOfWeek == DayOfWeek.MONDAY,
      tuesday = startDate.dayOfWeek == DayOfWeek.TUESDAY,
      wednesday = startDate.dayOfWeek == DayOfWeek.WEDNESDAY,
      thursday = startDate.dayOfWeek == DayOfWeek.THURSDAY,
      friday = startDate.dayOfWeek == DayOfWeek.FRIDAY,
      saturday = startDate.dayOfWeek == DayOfWeek.SATURDAY,
      sunday = startDate.dayOfWeek == DayOfWeek.SUNDAY,
      customStartTime = LocalTime.of(9, 45),
      customEndTime = LocalTime.of(11, 45),
    )

    val updatedActivity = webTestClient.updateActivity(
      activity.prisonCode,
      activity.id,
      ActivityUpdateRequest(
        slots = listOf(amSlot),
      ),
    )

    updatedActivity.schedules.flatMap { it.instances } hasSize 2

    with(updatedActivity.schedules.first().instances) {
      this.forEach {
        assertThat(it.startTime == LocalTime.of(9, 45))
        assertThat(it.endTime == LocalTime.of(11, 45))
        assertThat(it.timeSlot == TimeSlot.AM)
      }
    }
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-40.sql")
  fun `getActivityPayHistory - is successful`() {
    val activityPayHistoryList = webTestClient.getActivityPayHistory(1L)

    assertThat(activityPayHistoryList).size().isEqualTo(3)
    with(activityPayHistoryList[0]) {
      id.isEqualTo(3)
      incentiveNomisCode.isEqualTo("BAS")
      incentiveLevel.isEqualTo("Basic")
      prisonPayBand.id.isEqualTo(1)
      rate.isEqualTo(200)
      startDate.isEqualTo(LocalDate.parse("2025-05-07"))
      changedDetails.isEqualTo("Amount increased to £2.00, from 7 May 2025")
      changedTime.isEqualTo(LocalDateTime.parse("2025-04-20T09:00"))
      changedBy.isEqualTo("joebloggs")
    }

    with(activityPayHistoryList[1]) {
      id.isEqualTo(2)
      incentiveNomisCode.isEqualTo("BAS")
      incentiveLevel.isEqualTo("Basic")
      prisonPayBand.id.isEqualTo(1)
      rate.isEqualTo(75)
      startDate.isEqualTo(LocalDate.parse("2025-03-09"))
      changedDetails.isEqualTo("Amount reduced to £0.75, from 9 Mar 2025")
      changedTime.isEqualTo(LocalDateTime.parse("2025-04-10T09:00"))
      changedBy.isEqualTo("adsmith")
    }

    with(activityPayHistoryList[2]) {
      id.isEqualTo(1)
      incentiveNomisCode.isEqualTo("BAS")
      incentiveLevel.isEqualTo("Basic")
      prisonPayBand.id.isEqualTo(1)
      rate.isEqualTo(125)
      startDate.isEqualTo(null)
      changedDetails.isEqualTo("New pay rate added: £1.25")
      changedTime.isEqualTo(LocalDateTime.parse("2025-03-09T09:00"))
      changedBy.isEqualTo("joebloggs")
    }
  }
}
