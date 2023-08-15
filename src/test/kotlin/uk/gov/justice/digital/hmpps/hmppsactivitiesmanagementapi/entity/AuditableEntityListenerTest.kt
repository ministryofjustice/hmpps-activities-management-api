package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.enumeration.ServiceName
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.ActivityCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerAllocatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.audit.PrisonerDeallocatedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.AuditService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.FakeSecurityContext
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.addCaseloadIdToRequestHeader
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util.clearCaseloadIdFromRequestHeader
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@ExtendWith(FakeSecurityContext::class)
class AuditableEntityListenerTest(@Autowired private val listener: AuditableEntityListener) {

  @MockBean
  private lateinit var auditService: AuditService
  private val activity = activityEntity()
  private val allocation = allocation()

  private val activityCreatedCaptor = argumentCaptor<ActivityCreatedEvent>()
  private val prisonerAllocatedCaptor = argumentCaptor<PrisonerAllocatedEvent>()
  private val prisonerDeallocatedCaptor = argumentCaptor<PrisonerDeallocatedEvent>()

  private val caseLoad = "MDI"

  @BeforeEach
  fun setUp() {
    addCaseloadIdToRequestHeader(caseLoad)
  }

  @AfterEach
  fun tearDown() {
    clearCaseloadIdFromRequestHeader()
  }

  @Test
  fun `activity created event raised on creation`() {
    listener.onCreate(activity)

    verify(auditService).logEvent(activityCreatedCaptor.capture())
    verifyNoMoreInteractions(auditService)

    with(activityCreatedCaptor.firstValue) {
      assertThat(activityId).isEqualTo(1)
      assertThat(activityName).isEqualTo("Maths")
      assertThat(prisonCode).isEqualTo(caseLoad)
      assertThat(categoryCode).isEqualTo("category name")
      assertThat(startDate).isNotNull
      assertThat(createdAt).isNotNull
    }
  }

  @Test
  fun `prisoner deallocation audit event raised on update`() {
    listener.onUpdate(allocation.deallocateNow())

    verify(auditService).logEvent(prisonerDeallocatedCaptor.capture())
    verifyNoMoreInteractions(auditService)

    with(prisonerDeallocatedCaptor.firstValue) {
      assertThat(activityId).isEqualTo(1)
      assertThat(activityName).isEqualTo("Maths")
      assertThat(prisonCode).isEqualTo(caseLoad)
      assertThat(prisonerNumber).isEqualTo("A1234AA")
      assertThat(scheduleId).isEqualTo(1)
      assertThat(deallocatedBy).isEqualTo(ServiceName.SERVICE_NAME.value)
      assertThat(deallocationTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(reason).isEqualTo("Allocation end date reached")
      assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
    }
  }
}
