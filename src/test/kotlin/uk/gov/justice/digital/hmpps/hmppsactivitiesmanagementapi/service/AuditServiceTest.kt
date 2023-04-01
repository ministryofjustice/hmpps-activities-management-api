package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.LocalAuditRecord
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditableEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.BonusPaymentMadeForActivityAttendanceEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.SecurityTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AuditServiceTest {

  private val hmppsEventCaptor = argumentCaptor<HmppsAuditEvent>()

  private val localAuditRecordCaptor = argumentCaptor<LocalAuditRecord>()

  private val hmppsAuditApiClient = mock<HmppsAuditApiClient>()

  private val auditRepository = mock<AuditRepository>()

  private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  private val auditService = AuditService(hmppsAuditApiClient, auditRepository, true)

  @Test
  fun `should not log hmpps auditable event if feature is disabled`() {
    val event = mock<AuditableEvent>()
    val auditService = AuditService(hmppsAuditApiClient, auditRepository, false)

    auditService.logEvent(event)

    verify(hmppsAuditApiClient, never()).createEvent(any())
  }

  @Test
  fun `should log event correctly`() {
    val username = "Bob"
    SecurityTestUtils.setLoggedInUser(username)

    val event = BonusPaymentMadeForActivityAttendanceEvent(
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

    auditService.logEvent(event)

    verify(hmppsAuditApiClient).createEvent(hmppsEventCaptor.capture())
    with(hmppsEventCaptor.firstValue) {
      assertThat(who).isEqualTo(username)
      assertThat(what).isEqualTo(AuditEventType.BONUS_PAYMENT_MADE_FOR_ACTIVITY_ATTENDANCE.name)
      assertThat(details).isEqualTo("""{"activityId":1,"activityName":"Some Activity","prisonCode":"PBI","prisonerNumber":"AA12346","scheduleId":1,"date":"2023-01-02","startTime":"10:00:00","endTime":"11:00:00","createdAt":"2023-01-02T13:43:56","createdBy":"Bob"}""")
      assertThat(service).isEqualTo("hmpps-activities-management-api")
      assertThat(`when`).isNotNull
    }

    verify(auditRepository).save(localAuditRecordCaptor.capture())
    with(localAuditRecordCaptor.firstValue) {
      assertThat(`username`).isEqualTo(username)
      assertThat(auditType).isEqualTo(AuditType.PRISONER)
      assertThat(detailType).isEqualTo(AuditEventType.BONUS_PAYMENT_MADE_FOR_ACTIVITY_ATTENDANCE)
      assertThat(prisonCode).isEqualTo("PBI")
      assertThat(prisonerNumber).isEqualTo("AA12346")
      assertThat(activityId).isEqualTo(1)
      assertThat(activityScheduleId).isEqualTo(1)
      assertThat(message).isEqualTo("A bonus payment was made to prisoner AA12346 for activity 'Some Activity'(1) scheduled on 2023-01-02 between 10:00 and 11:00 (scheduleId = 1). Event created on 2023-01-02 at 13:43:56 by Bob.")
    }
  }
}
