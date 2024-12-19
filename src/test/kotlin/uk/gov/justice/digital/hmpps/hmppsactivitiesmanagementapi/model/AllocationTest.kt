package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.DeallocationReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.Allocation as EntityAllocation

class AllocationTest : ModelTest() {

  @Test
  fun `dates and times are serialized correctly`() {
    val originalStartDate = LocalDate.parse("01 Feb 2023", dateFormatter)
    val originalEndDate = LocalDate.parse("07 Feb 2023", dateFormatter)
    val originalAllocatedTime = LocalDateTime.parse("31 Jan 2023 10:21:22", dateTimeFormatter)
    val originalDeallocatedTime = LocalDateTime.parse("31 Jan 2023 12:13:14", dateTimeFormatter)

    val expectedStartDate = "2023-02-01"
    val expectedEndDate = "2023-02-07"
    val expectedAllocatedTime = "2023-01-31T10:21:22"
    val expectedDeallocatedTime = "2023-01-31T12:13:14"

    val allocation = Allocation(
      id = 1,
      startDate = originalStartDate,
      endDate = originalEndDate,
      allocatedTime = originalAllocatedTime,
      deallocatedTime = originalDeallocatedTime,
      activitySummary = "Blah",
      bookingId = 123,
      prisonPayBand = PrisonPayBand(
        id = lowPayBand.prisonPayBandId,
        displaySequence = lowPayBand.displaySequence,
        alias = lowPayBand.payBandAlias,
        description = lowPayBand.payBandDescription,
        prisonCode = lowPayBand.prisonCode,
        nomisPayBand = lowPayBand.nomisPayBand,
      ),
      prisonerNumber = "1234",
      scheduleDescription = "Blah blah",
      activityId = 123,
      scheduleId = 123,
      status = PrisonerStatus.ACTIVE,
      plannedDeallocation = PlannedDeallocation(
        id = 1,
        plannedDate = LocalDate.of(2022, 10, 11),
        plannedBy = "MR BLOGSS",
        plannedReason = DeallocationReason.PLANNED.toModel(),
        plannedAt = LocalDateTime.now(),
      ),
      exclusions = emptyList(),
    )

    val json = objectMapper.writeValueAsString(allocation)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["startDate"]).isEqualTo(expectedStartDate)
    assertThat(jsonMap["endDate"]).isEqualTo(expectedEndDate)
    assertThat(jsonMap["allocatedTime"]).isEqualTo(expectedAllocatedTime)
    assertThat(jsonMap["deallocatedTime"]).isEqualTo(expectedDeallocatedTime)
  }

  @Test
  fun `check active allocation transformation`() {
    val allocation = allocation().also { assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ACTIVE) }

    with(allocation.toModel()) {
      assertOnCommonModalTransformation(this, allocation)
      assertThat(deallocatedBy).isNull()
      assertThat(deallocatedReason).isNull()
      assertThat(deallocatedTime).isNull()
      assertThat(suspendedBy).isNull()
      assertThat(suspendedReason).isNull()
      assertThat(suspendedTime).isNull()
    }
  }

  @Test
  fun `check auto suspended allocation transformation`() {
    val now = LocalDateTime.now()

    val allocation = allocation().also {
      it.autoSuspend(now, "auto suspend reason")
      assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.AUTO_SUSPENDED)
    }

    with(allocation.toModel()) {
      assertOnCommonModalTransformation(this, allocation)
      assertThat(suspendedBy).isEqualTo("Activities Management Service")
      assertThat(suspendedReason).isEqualTo("auto suspend reason")
      assertThat(suspendedTime).isEqualTo(now)
      assertThat(deallocatedBy).isNull()
      assertThat(deallocatedReason).isNull()
      assertThat(deallocatedTime).isNull()
    }
  }

  @Test
  fun `check user suspended allocation transformation`() {
    val now = LocalDateTime.now()

    val allocation = allocation(withPlannedSuspensions = true).also {
      it.activatePlannedSuspension()
      assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.SUSPENDED)
    }

    with(allocation.toModel()) {
      assertOnCommonModalTransformation(this, allocation)
      suspendedBy isEqualTo "Test"
      suspendedReason isEqualTo "Planned suspension"
      suspendedTime isCloseTo now
      assertThat(deallocatedBy).isNull()
      assertThat(deallocatedReason).isNull()
      assertThat(deallocatedTime).isNull()
      plannedSuspension isEqualTo allocation.plannedSuspension()?.toModel()
    }
  }

  @Test
  fun `check user suspended paid allocation transformation`() {
    val now = LocalDateTime.now()

    val allocation = allocation(withPlannedSuspensions = true, withPaidSuspension = true).also {
      it.activatePlannedSuspension()
      assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.SUSPENDED_WITH_PAY)
    }

    with(allocation.toModel()) {
      assertOnCommonModalTransformation(this, allocation)
      suspendedBy isEqualTo "Test"
      suspendedReason isEqualTo "Planned suspension"
      suspendedTime isCloseTo now
      assertThat(deallocatedBy).isNull()
      assertThat(deallocatedReason).isNull()
      assertThat(deallocatedTime).isNull()
      plannedSuspension isEqualTo allocation.plannedSuspension()?.toModel()
    }
  }

  @Test
  fun `check deallocated allocation transformation`() {
    val allocation = allocation().also {
      it.deallocateNowWithReason(DeallocationReason.ENDED)
      assertThat(it.prisonerStatus).isEqualTo(PrisonerStatus.ENDED)
    }

    with(allocation.toModel()) {
      assertOnCommonModalTransformation(this, allocation)
      assertThat(deallocatedBy).isEqualTo("Activities Management Service")
      assertThat(deallocatedReason).isEqualTo(DeallocationReason.ENDED.toModel())
      assertThat(deallocatedTime).isCloseTo(LocalDateTime.now(), within(2, ChronoUnit.SECONDS))
      assertThat(suspendedBy).isNull()
      assertThat(suspendedReason).isNull()
      assertThat(suspendedTime).isNull()
    }
  }

  @Test
  fun `check displayable deallocation reasons`() {
    assertThat(DeallocationReason.toModelDeallocationReasons()).hasSize(8)

    assertThat(DeallocationReason.toModelDeallocationReasons()).containsExactlyInAnyOrder(
      DeallocationReason.COMPLETED.toModel(),
      DeallocationReason.HEALTH.toModel(),
      DeallocationReason.OTHER.toModel(),
      DeallocationReason.SECURITY.toModel(),
      DeallocationReason.TRANSFERRED.toModel(),
      DeallocationReason.WITHDRAWN_OWN.toModel(),
      DeallocationReason.WITHDRAWN_STAFF.toModel(),
      DeallocationReason.DISMISSED.toModel(),
    )
  }

  private fun assertOnCommonModalTransformation(model: Allocation, entity: EntityAllocation) {
    with(model) {
      assertThat(id).isEqualTo(entity.allocationId)
      assertThat(prisonerNumber).isEqualTo(entity.prisonerNumber)
      assertThat(bookingId).isEqualTo(entity.bookingId)
      assertThat(activitySummary).isEqualTo(entity.activitySchedule.activity.summary)
      assertThat(scheduleId).isEqualTo(entity.activitySchedule.activityScheduleId)
      assertThat(scheduleDescription).isEqualTo(entity.activitySchedule.description)
      assertThat(isUnemployment).isEqualTo(entity.activitySchedule.activity.isUnemployment())
      assertThat(startDate).isEqualTo(entity.startDate)
      assertThat(endDate).isEqualTo(entity.endDate)
      assertThat(allocatedTime).isEqualTo(entity.allocatedTime)
      assertThat(allocatedBy).isEqualTo(entity.allocatedBy)
      assertThat(prisonPayBand).isEqualTo(entity.payBand?.toModel())
    }
  }
}
