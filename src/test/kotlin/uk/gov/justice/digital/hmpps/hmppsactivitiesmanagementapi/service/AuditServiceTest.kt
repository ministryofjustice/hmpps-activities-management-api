package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.BonusPaymentMadeForActivityAttendanceEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.request.AuditRecordSearchFilters
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.DEFAULT_USERNAME
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@ExtendWith(FakeSecurityContext::class)
class AuditServiceTest {

  private val hmppsEventCaptor = argumentCaptor<HmppsAuditEvent>()

  private val localAuditRecordCaptor = argumentCaptor<LocalAuditRecord>()

  private val hmppsAuditApiClient = mock<HmppsAuditApiClient>()

  private val auditRepository = mock<AuditRepository>()

  private val featureSwitches: FeatureSwitches = mock { on { isEnabled(any<Feature>(), any()) } doReturn true }

  private val auditService = AuditService(hmppsAuditApiClient, auditRepository, featureSwitches)

  @Nested
  @DisplayName("searchEvents")
  inner class SearchEvents {

    @Test
    fun `applies filters correctly`() {
      val page = 0
      val size = 10
      val sortDirection = "ascending"
      val prisonCode = "PVI"
      val prisonerNumber = "A12346"
      val username = "Bob"
      val auditType = AuditType.ACTIVITY
      val auditEventType = AuditEventType.ACTIVITY_CREATED
      val startTime = LocalDateTime.now()
      val endTime = LocalDateTime.now()
      val activityId = 1L
      val scheduleId = 2L

      val repositoryResult = PageImpl(
        listOf(
          LocalAuditRecord(
            localAuditId = 1,
            username = "Bob",
            auditType = AuditType.ACTIVITY,
            detailType = AuditEventType.ACTIVITY_CREATED,
            prisonCode = "PVI",
            recordedTime = LocalDateTime.of(2022, 1, 2, 9, 2, 3),
            message = "An activity was created",
          ),
        ),
      )

      val filters = AuditRecordSearchFilters(
        prisonCode = prisonCode,
        prisonerNumber = prisonerNumber,
        username = username,
        auditType = auditType,
        auditEventType = auditEventType,
        startTime = startTime,
        endTime = endTime,
        activityId = activityId,
        scheduleId = scheduleId,
      )

      whenever(
        auditRepository.searchRecords(
          prisonCode = eq(prisonCode),
          prisonerNumber = eq(prisonerNumber),
          username = eq(username),
          auditType = eq(auditType),
          auditEventType = eq(auditEventType),
          startTime = eq(startTime),
          endTime = eq(endTime),
          activityId = eq(activityId),
          scheduleId = eq(scheduleId),
          pageable = any(),
        ),
      ).thenReturn(repositoryResult)

      val result = auditService.searchEvents(page, size, sortDirection, filters)

      assertThat(result.totalElements).isEqualTo(1)
      assertThat(result.content).hasSize(1)
      with(result.content.first()) {
        assertThat(localAuditId).isEqualTo(1)
        assertThat(username).isEqualTo("Bob")
        assertThat(auditType).isEqualTo(AuditType.ACTIVITY)
        assertThat(auditEventType).isEqualTo(AuditEventType.ACTIVITY_CREATED)
        assertThat(prisonCode).isEqualTo("PVI")
        assertThat(recordedTime).isEqualTo(LocalDateTime.of(2022, 1, 2, 9, 2, 3))
        assertThat(message).isEqualTo("An activity was created")
      }
    }
  }

  @Nested
  @DisplayName("logEvent")
  inner class LogEvent {
    @Test
    fun `should not log hmpps auditable event if feature is disabled`() {
      val event = createEvent()

      featureSwitches.stub { on { isEnabled(Feature.HMPPS_AUDIT_ENABLED) } doReturn false }
      AuditService(hmppsAuditApiClient, auditRepository, featureSwitches).logEvent(event)

      verify(hmppsAuditApiClient, never()).createEvent(any())
      verify(auditRepository).save(any())
    }

    @Test
    fun `should not log local auditable event if feature is disabled`() {
      val event = createEvent()

      featureSwitches.stub { on { isEnabled(Feature.LOCAL_AUDIT_ENABLED) } doReturn false }
      AuditService(hmppsAuditApiClient, auditRepository, featureSwitches).logEvent(event)

      verify(hmppsAuditApiClient).createEvent(any())
      verify(auditRepository, never()).save(any())
    }

    @Test
    fun `should log event correctly`() {
      val event = createEvent()

      auditService.logEvent(event)

      verify(hmppsAuditApiClient).createEvent(hmppsEventCaptor.capture())
      with(hmppsEventCaptor.firstValue) {
        assertThat(who).isEqualTo(DEFAULT_USERNAME)
        assertThat(what).isEqualTo(AuditEventType.BONUS_PAYMENT_MADE_FOR_ACTIVITY_ATTENDANCE.name)
        assertThat(details).isEqualTo("""{"activityId":1,"activityName":"Some Activity","prisonCode":"PBI","prisonerNumber":"AA12346","scheduleId":1,"date":"2023-01-02","startTime":"10:00:00","endTime":"11:00:00","createdAt":"2023-01-02T13:43:56","createdBy":"Bob"}""")
        assertThat(service).isEqualTo("hmpps-activities-management-api")
        assertThat(`when`).isNotNull
      }

      verify(auditRepository).save(localAuditRecordCaptor.capture())
      with(localAuditRecordCaptor.firstValue) {
        assertThat(username).isEqualTo("Bob")
        assertThat(auditType).isEqualTo(AuditType.PRISONER)
        assertThat(detailType).isEqualTo(AuditEventType.BONUS_PAYMENT_MADE_FOR_ACTIVITY_ATTENDANCE)
        assertThat(prisonCode).isEqualTo("PBI")
        assertThat(prisonerNumber).isEqualTo("AA12346")
        assertThat(activityId).isEqualTo(1)
        assertThat(activityScheduleId).isEqualTo(1)
        assertThat(message).isEqualTo("A bonus payment was made to prisoner AA12346 for activity 'Some Activity'(1) scheduled on 2023-01-02 between 10:00 and 11:00 (scheduleId = 1). Event created on 2023-01-02 at 13:43:56 by Bob.")
      }
    }

    private fun createEvent() = BonusPaymentMadeForActivityAttendanceEvent(
      1,
      "Some Activity",
      "PBI",
      "AA12346",
      1,
      LocalDate.of(2023, 1, 2),
      LocalTime.of(10, 0),
      LocalTime.of(11, 0),
      LocalDateTime.of(2023, 1, 2, 13, 43, 56),
    )
  }
}
