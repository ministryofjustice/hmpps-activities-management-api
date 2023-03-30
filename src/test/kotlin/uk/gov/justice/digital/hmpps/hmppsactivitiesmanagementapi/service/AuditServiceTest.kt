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
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditableEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.BonusPaymentMadeForActivityAttendanceEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AuditRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.SecurityTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AuditServiceTest {

  private val hmppsEventCaptor = argumentCaptor<HmppsAuditEvent>()

  private val hmppsAuditApiClient = mock<HmppsAuditApiClient>()

  private val auditRepository = mock<AuditRepository>()

  private val objectMapper = jacksonObjectMapper()
    .registerModule(JavaTimeModule())
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

  private val auditService = AuditService(hmppsAuditApiClient, auditRepository, objectMapper, true)

  @Test
  fun `should not log hmpps auditable event if feature is disabled`() {
    val event = mock<AuditableEvent>()
    val auditService = AuditService(hmppsAuditApiClient, auditRepository, objectMapper, false)

    auditService.logEvent(event)

    verify(hmppsAuditApiClient, never()).createEvent(any())
  }

  @Test
  fun `should log hmpps auditable event correctly`() {
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
  }
}
