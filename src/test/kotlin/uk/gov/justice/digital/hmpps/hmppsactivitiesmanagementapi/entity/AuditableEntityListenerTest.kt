package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.ActivityCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerAllocatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@ExtendWith(FakeSecurityContext::class)
class AuditableEntityListenerTest(@Autowired private val listener: AuditableListener) {

  @MockBean
  private lateinit var auditService: AuditService
  private val activity = activityEntity()
  private val allocation = allocation()

  private val activityCreatedCaptor = argumentCaptor<ActivityCreatedEvent>()
  private val prisonerAllocatedCaptor = argumentCaptor<PrisonerAllocatedEvent>()

  @Test
  fun `activity created event raised on creation`() {
    listener.onCreate(activity)

    verify(auditService).logEvent(activityCreatedCaptor.capture())
    verifyNoMoreInteractions(auditService)

    with(activityCreatedCaptor.firstValue) {
      assertThat(activityId).isEqualTo(1)
      assertThat(activityName).isEqualTo("Maths")
      assertThat(prisonCode).isEqualTo("123")
      assertThat(categoryCode).isEqualTo("category name")
      assertThat(startDate).isNotNull
      assertThat(createdAt).isNotNull
    }
  }

  @Test
  fun `prisoner allocation audit event raised on creation`() {
    listener.onCreate(allocation)

    verify(auditService).logEvent(prisonerAllocatedCaptor.capture())
    verifyNoMoreInteractions(auditService)

    with(prisonerAllocatedCaptor.firstValue) {
      assertThat(activityId).isEqualTo(1)
      assertThat(activityName).isEqualTo("Maths")
      assertThat(prisonCode).isEqualTo("123")
      assertThat(prisonerNumber).isEqualTo("A1234AA")
      assertThat(scheduleId).isEqualTo(1)
      assertThat(scheduleDescription).isNotNull
      assertThat(createdAt).isNotNull
    }
  }
}
