package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.pentonvillePrisonCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.repository.AllocationRepository
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.InboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.InboundEventsProcessor
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.OutboundEventsService

/**
 * This integration test is bypassing the step whereby this would be instigated by incoming prisoner events.
 */
class InboundEventsIntegrationTest : IntegrationTestBase() {

  @MockBean
  private lateinit var outboundEventsService: OutboundEventsService

  @Autowired
  private lateinit var repository: AllocationRepository

  @Autowired
  private lateinit var service: InboundEventsProcessor

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `temporary release of prisoner`() {
    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    service.process(InboundEvent.OFFENDER_TEMPORARILY_RELEASED, pentonvillePrisonCode, "A11111A")

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.AUTO_SUSPENDED))
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `receive prisoner back after temporary release`() {
    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    service.process(InboundEvent.OFFENDER_TEMPORARILY_RELEASED, pentonvillePrisonCode, "A11111A")

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.AUTO_SUSPENDED))
    }

    service.process(InboundEvent.OFFENDER_RECEIVED, pentonvillePrisonCode, "A11111A")

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    verify(outboundEventsService, times(2)).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService, times(2)).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)
  }

  @Sql(
    "classpath:test_data/seed-activity-id-1.sql",
  )
  @Test
  fun `permanent release of prisoner`() {
    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ACTIVE))
    }

    service.process(InboundEvent.OFFENDER_RELEASED, pentonvillePrisonCode, "A11111A")

    repository.findByPrisonCodeAndPrisonerNumber(pentonvillePrisonCode, "A11111A").onEach {
      assertThat(it.status(PrisonerStatus.ENDED))
    }

    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 1L)
    verify(outboundEventsService).send(OutboundEvent.PRISONER_ALLOCATION_AMENDED, 4L)
  }
}
