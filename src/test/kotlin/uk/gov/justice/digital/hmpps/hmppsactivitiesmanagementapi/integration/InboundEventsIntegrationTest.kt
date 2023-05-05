package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.EventReviewRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.InboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OffenderReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.ReleaseInformation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReceivedFromTemporaryAbsence
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.offenderTemporaryReleasedEvent

/**
 * This integration test is bypassing the step whereby this would be instigated by incoming prisoner events.
 */
class InboundEventsIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var outboundEventsService: OutboundEventsService

  @Autowired
  private lateinit var repository: AllocationRepository

  @Autowired
  private lateinit var eventReviewRepository: EventReviewRepository

  @Autowired
  private lateinit var service: InboundEventsService

  @BeforeEach
  fun initMocks() {
    reset(outboundEventsService)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `temporary release of prisoner`() {
    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    service.process(offenderTemporaryReleasedEvent(prisonerNumber = "A11111A"))

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.AUTO_SUSPENDED))
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `release with unknown reason does not affect allocations but is treated as an interesting event`() {
    assertThat(eventReviewRepository.count()).isEqualTo(0)
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber = "A11111A", fullInfo = false)

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    // This event falls back to being processed as an interesting event due to the unknown reason for release
    service.process(
      OffenderReleasedEvent(
        ReleaseInformation(
          nomsNumber = "A11111A",
          reason = "UNKNOWN",
          prisonId = pentonvillePrisonCode,
        ),
      ),
    )

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    assertThat(eventReviewRepository.count()).isEqualTo(1L)
    verifyNoInteractions(outboundEventsService)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `receive prisoner back after temporary release`() {
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber = "A11111A", fullInfo = true)

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    service.process(offenderTemporaryReleasedEvent(prisonerNumber = "A11111A"))

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.AUTO_SUSPENDED))
    }

    service.process(offenderReceivedFromTemporaryAbsence(prisonerNumber = "A11111A"))

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    verify(outboundEventsService, times(2)).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService, times(2)).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `permanent release of prisoner from remand`() {
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber = "A11111A", fullInfo = true, extraInfo = true, jsonFileSuffix = "-released-from-remand")

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    service.process(offenderReleasedEvent(prisonerNumber = "A11111A"))

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ENDED))
      assertThat(it.deallocatedReason).isEqualTo("Released")
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `permanent release of prisoner from custodial sentence`() {
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber = "A11111A", fullInfo = true, extraInfo = true, jsonFileSuffix = "-released-from-custodial-sentence")

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    service.process(offenderReleasedEvent(prisonerNumber = "A11111A"))

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ENDED))
      assertThat(it.deallocatedReason).isEqualTo("Released")
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)
  }

  @Test
  @Sql("classpath:test_data/seed-activity-id-1.sql")
  fun `permanent release of prisoner due to death in prison`() {
    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber = "A11111A", fullInfo = true, extraInfo = true, jsonFileSuffix = "-released-on-death-in-prison")

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    service.process(offenderReleasedEvent(prisonerNumber = "A11111A"))

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ENDED))
      assertThat(it.deallocatedReason).isEqualTo("Dead")
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)
  }
}
