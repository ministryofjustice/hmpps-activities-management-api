package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.api.HmppsAuditApiClient
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.audit.model.HmppsAuditEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditEventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.AuditableEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.BonusPaymentMadeForActivityAttendanceEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.SecurityTestUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class AuditServiceTest {

  private val hmppsEventCaptor = argumentCaptor<HmppsAuditEvent>()

  private val hmppsAuditApiClient = mock<HmppsAuditApiClient>()

  private val auditService = AuditService(hmppsAuditApiClient, true)

  @Test
  fun `should not log hmpps auditable event if feature is disabled`() {
    val event = mock<AuditableEvent>()
    val auditService = AuditService(hmppsAuditApiClient, false)

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
      "P2",
      "Terry",
      "Jones",
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
      assertThat(details).isEqualTo("""{"activityId":1,"activityName":"Some Activity","prisonerNumber":"P2","prisonerFirstName":"Terry","prisonerLastName":"Jones","scheduleId":1,"date":"2023-01-02","startTime":"10:00:00","endTime":"11:00:00","createdAt":"2023-01-02T13:43:56","createdBy":"Bob"}""")
      assertThat(service).isEqualTo("hmpps-activities-management-api")
      assertThat(`when`).isNotNull
    }
  }
}
