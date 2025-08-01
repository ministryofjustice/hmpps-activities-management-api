package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.api.extensions.toModel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.PrisonerNonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AttendanceStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerScheduledActivity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.ScheduledInstanceAttendanceSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingList
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.WaitingListStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.appointment.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.AttendanceReasonEnum
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.refdata.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.PENTONVILLE_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.TimeSource
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.advanceAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentInstanceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentSearchEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.earliestReleaseDate
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.locationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Slot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.OtherPrisonerDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService.LocationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.EventPriorities
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.refdata.Priority
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityEligibility as ModelActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel as ModelActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AdvanceAttendance as ModelAdvanceAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentFrequency as ModelAppointmentFrequency
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EligibilityRule as ModelEligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventOrganiser as ModelEventOrganiser
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EventTier as ModelEventTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent as ModelScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstance as ModelScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

class TransformFunctionsTest {
  @Test
  fun `transformation of activity entity to the activity models`() {
    val timestamp = LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 0)).truncatedTo(ChronoUnit.MINUTES)
    val timeSlot = TimeSlot.AM
    val activity = activityEntity(timestamp = timestamp, timeSlot = TimeSlot.AM).apply { attendanceRequired = false }

    with(transform(activity)) {
      assertThat(id).isEqualTo(1)
      assertThat(prisonCode).isEqualTo("MDI")
      assertThat(attendanceRequired).isFalse
      assertThat(summary).isEqualTo("Maths")
      assertThat(description).isEqualTo("Maths basic")
      assertThat(category).isEqualTo(
        ModelActivityCategory(
          id = 1,
          code = "category code",
          name = "category name",
          description = "category description",
        ),
      )
      assertThat(tier).isEqualTo(ModelEventTier(2, "TIER_2", "Tier 2"))
      assertThat(organiser).isEqualTo(
        ModelEventOrganiser(
          id = 1,
          code = "PRISON_STAFF",
          description = "Prison staff",
        ),
      )
      assertThat(eligibilityRules).containsExactly(
        ModelActivityEligibility(
          0,
          ModelEligibilityRule(
            1,
            code = "OVER_21",
            description = "The prisoner must be over 21 to attend",
          ),
        ),
      )
      assertThat(schedules).containsExactly(
        ModelActivitySchedule(
          id = 1,
          instances = listOf(
            ModelScheduledInstance(
              id = 0,
              date = timestamp.toLocalDate(),
              startTime = timestamp.toLocalTime(),
              endTime = timestamp.toLocalTime().plusHours(1),
              timeSlot = timeSlot,
              cancelled = false,
              attendances = listOf(
                ModelAttendance(
                  id = 1,
                  scheduleInstanceId = 0,
                  prisonerNumber = "A1234AA",
                  status = "WAITING",
                  issuePayment = null,
                  incentiveLevelWarningIssued = null,
                  recordedTime = LocalDate.now().atStartOfDay(),
                  recordedBy = "Joe Bloggs",
                  editable = true,
                  payable = true,
                  attendanceHistory = null,
                ),
              ),
              advanceAttendances = listOf(
                ModelAdvanceAttendance(
                  id = 1,
                  scheduleInstanceId = 0,
                  prisonerNumber = "A1234AA",
                  issuePayment = true,
                  payAmount = null,
                  recordedTime = LocalDate.now().atStartOfDay(),
                  recordedBy = "Joe Bloggs",
                  attendanceHistory = null,
                ),
              ),
            ),
          ),
          internalLocation = InternalLocation(1, "EDU-ROOM-1", "Education - R1", UUID.fromString("99999999-0000-aaaa-bbbb-cccccccccccc")),
          allocations = listOf(
            Allocation(
              id = 0,
              prisonerNumber = "A1234AA",
              bookingId = 10001,
              prisonPayBand = lowPayBand.toModel(),
              startDate = timestamp.toLocalDate(),
              endDate = null,
              allocatedTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES),
              allocatedBy = "Mr Blogs",
              activitySummary = "Maths",
              activityId = 1,
              scheduleId = 1,
              scheduleDescription = "schedule description",
              status = PrisonerStatus.ACTIVE,
              plannedDeallocation = null,
              exclusions = emptyList(),
            ),
            Allocation(
              id = 0,
              prisonerNumber = "A1111BB",
              bookingId = 20002,
              prisonPayBand = lowPayBand.toModel(),
              startDate = timestamp.toLocalDate(),
              endDate = null,
              allocatedTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES),
              allocatedBy = "Mr Blogs",
              activitySummary = "Maths",
              activityId = 1,
              scheduleId = 1,
              scheduleDescription = "schedule description",
              status = PrisonerStatus.ACTIVE,
              plannedDeallocation = null,
              exclusions = listOf(
                Slot(
                  weekNumber = 1,
                  timeSlot = timeSlot,
                  monday = true,
                  tuesday = false,
                  wednesday = false,
                  thursday = false,
                  friday = false,
                  saturday = false,
                  sunday = false,
                ),
              ),
            ),
          ),
          description = "schedule description",
          capacity = 1,
          activity = activity.toModelLite(),
          slots = listOf(
            ActivityScheduleSlot(
              id = 0,
              timeSlot = timeSlot,
              weekNumber = 1,
              startTime = timestamp.toLocalTime(),
              endTime = timestamp.toLocalTime().plusHours(1),
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
          startDate = activity.startDate,
          runsOnBankHoliday = false,
          updatedTime = null,
          updatedBy = null,
          scheduleWeeks = 1,
          usePrisonRegimeTime = true,
        ),
      )
      assertThat(pay).containsExactly(
        ModelActivityPay(
          id = 0,
          incentiveNomisCode = "BAS",
          incentiveLevel = "Basic",
          prisonPayBand = lowPayBand.toModelPrisonPayBand(),
          rate = 30,
          pieceRate = 40,
          pieceRateItems = 50,
        ),
      )
      assertThat(startDate).isEqualTo(timestamp.toLocalDate())
      assertThat(endDate).isNull()
      assertThat(createdTime).isEqualTo(timestamp)
      assertThat(createdBy).isEqualTo("test")
      assertThat(minimumEducationLevel).containsExactly(
        ModelActivityMinimumEducationLevel(
          id = 0,
          educationLevelCode = "1",
          educationLevelDescription = "Reading Measure 1.0",
          studyAreaCode = "ENGLA",
          studyAreaDescription = "English Language",
        ),
      )
    }
  }

  @Nested
  @DisplayName("prisonerScheduledActivityToScheduledEvents")
  inner class PrisonerScheduledActivityToScheduledEvents {
    @Test
    fun `activity instance, to scheduled event`() {
      val eventPriorities = EventPriorities(EventType.entries.associateWith { listOf(Priority(it.defaultPriority)) })

      val sessionDate = LocalDate.now()
      val startTime = LocalTime.of(10, 0)
      val endTime = LocalTime.of(11, 0)

      val activitiesForPrisoners = listOf(
        PrisonerScheduledActivity(
          scheduledInstanceId = 1,
          allocationId = 2,
          prisonCode = "RSI",
          sessionDate = sessionDate,
          startTime = startTime,
          endTime = endTime,
          prisonerNumber = "ABC123",
          bookingId = 90001,
          internalLocationId = 333,
          internalLocationCode = "LOC123",
          internalLocationDescription = "LOC123",
          inCell = true,
          onWing = true,
          offWing = true,
          activityCategory = "SAA_OUT_OF_WORK",
          activityId = 1,
          timeSlot = TimeSlot.AM,
          issuePayment = true,
          attendanceStatus = AttendanceStatus.COMPLETED,
          attendanceReasonCode = AttendanceReasonEnum.ATTENDED,
          paidActivity = true,
          cancelled = true,
          scheduleDescription = "Test Category",
          possibleAdvanceAttendance = true,
        ),
      )

      val location = Location(
        locationId = 1, locationType = "VIDE", description = "video-room-a", locationUsage = "APP",
        agencyId = "MDI", parentLocationId = 123, currentOccupancy = 2, locationPrefix = "MDI-prefix",
        operationalCapacity = 2, userDescription = "Video Room A", internalLocationCode = "Room 1",
      )

      val prisonLocations = mapOf(333L to location)

      val scheduledEvents = transformPrisonerScheduledActivityToScheduledEvents("RSI", eventPriorities, activitiesForPrisoners, prisonLocations)

      assertThat(scheduledEvents.first()).isEqualTo(
        ModelScheduledEvent(
          prisonCode = "RSI",
          eventSource = "SAA",
          eventType = EventType.ACTIVITY.name,
          scheduledInstanceId = 1,
          bookingId = 90001,
          internalLocationId = 333,
          internalLocationCode = "LOC123",
          internalLocationUserDescription = "Video Room A",
          internalLocationDescription = "video-room-a",
          eventId = null,
          appointmentSeriesId = null,
          appointmentId = null,
          appointmentAttendeeId = null,
          oicHearingId = null,
          cancelled = true,
          suspended = false,
          categoryCode = "SAA_OUT_OF_WORK",
          categoryDescription = "SAA_OUT_OF_WORK",
          summary = "Test Category",
          comments = null,
          prisonerNumber = "ABC123",
          inCell = true,
          onWing = true,
          offWing = true,
          outsidePrison = false,
          date = sessionDate,
          startTime = startTime,
          endTime = endTime,
          priority = EventType.ACTIVITY.defaultPriority,
          appointmentSeriesCancellationStartDate = null,
          appointmentSeriesCancellationStartTime = null,
          appointmentSeriesFrequency = null,
          paidActivity = true,
          issuePayment = true,
          attendanceStatus = "COMPLETED",
          attendanceReasonCode = "ATTENDED",
        ),
      )
    }
  }

  @Nested
  @DisplayName("appointmentInstanceToScheduledEvents")
  inner class AppointmentInstanceToScheduledEvents {
    @Test
    fun `appointment instance, with location, to scheduled event`() {
      val entity = appointmentInstanceEntity()
      val scheduledEvents = transform(entity, emptyMap())

      assertThat(scheduledEvents.first()).isEqualTo(
        ModelScheduledEvent(
          prisonCode = "TPR",
          eventSource = "SAA",
          eventType = EventType.APPOINTMENT.name,
          scheduledInstanceId = null,
          bookingId = entity.bookingId,
          internalLocationId = entity.internalLocationId,
          internalLocationCode = "No information available",
          internalLocationUserDescription = "No information available",
          internalLocationDescription = "No information available",
          eventId = null,
          appointmentSeriesId = entity.appointmentSeriesId,
          appointmentId = entity.appointmentId,
          appointmentAttendeeId = entity.appointmentInstanceId,
          oicHearingId = null,
          cancelled = entity.isCancelled,
          suspended = false,
          categoryCode = entity.categoryCode,
          categoryDescription = "Test Category",
          summary = "Test Category",
          comments = entity.extraInformation,
          prisonerNumber = entity.prisonerNumber,
          inCell = entity.inCell,
          outsidePrison = false,
          date = entity.appointmentDate,
          startTime = entity.startTime,
          endTime = entity.endTime,
          priority = EventType.APPOINTMENT.defaultPriority,
          appointmentSeriesCancellationStartDate = null,
          appointmentSeriesCancellationStartTime = null,
          appointmentSeriesFrequency = null,
          paidActivity = null,
          issuePayment = null,
          attendanceStatus = null,
          attendanceReasonCode = null,
        ),
      )
    }

    @Test
    fun `in cell appointment instance to scheduled event`() {
      val entity = appointmentInstanceEntity(inCell = true)
      val scheduledEvents = transform(entity)

      assertThat(scheduledEvents.first()).isEqualTo(
        ModelScheduledEvent(
          prisonCode = "TPR",
          eventSource = "SAA",
          eventType = EventType.APPOINTMENT.name,
          scheduledInstanceId = null,
          bookingId = entity.bookingId,
          internalLocationId = entity.internalLocationId,
          internalLocationCode = "In cell",
          internalLocationUserDescription = "In cell",
          internalLocationDescription = "In cell",
          eventId = null,
          appointmentSeriesId = entity.appointmentSeriesId,
          appointmentId = entity.appointmentId,
          appointmentAttendeeId = entity.appointmentInstanceId,
          oicHearingId = null,
          cancelled = entity.isCancelled,
          suspended = false,
          categoryCode = entity.categoryCode,
          categoryDescription = "Test Category",
          summary = "Test Category",
          comments = entity.extraInformation,
          prisonerNumber = entity.prisonerNumber,
          inCell = entity.inCell,
          outsidePrison = false,
          date = entity.appointmentDate,
          startTime = entity.startTime,
          endTime = entity.endTime,
          priority = EventType.APPOINTMENT.defaultPriority,
          appointmentSeriesCancellationStartDate = null,
          appointmentSeriesCancellationStartTime = null,
          appointmentSeriesFrequency = null,
          paidActivity = null,
          issuePayment = null,
          attendanceStatus = null,
          attendanceReasonCode = null,
        ),
      )
    }

    @Test
    fun `appointment instance where the series is cancelled`() {
      val entity = appointmentInstanceEntity(isCancelled = true)
      entity.seriesCancellationStartDate = LocalDate.of(2024, 5, 12)
      entity.seriesCancellationStartTime = LocalTime.of(10, 20)
      entity.seriesFrequency = AppointmentFrequency.WEEKLY

      val scheduledEvents = transform(entity)

      assertThat(scheduledEvents.first()).isEqualTo(
        ModelScheduledEvent(
          prisonCode = "TPR",
          eventSource = "SAA",
          eventType = EventType.APPOINTMENT.name,
          scheduledInstanceId = null,
          bookingId = entity.bookingId,
          internalLocationId = entity.internalLocationId,
          internalLocationCode = "LOC123",
          internalLocationUserDescription = "An appointment location",
          internalLocationDescription = "An appointment location",
          eventId = null,
          appointmentSeriesId = entity.appointmentSeriesId,
          appointmentId = entity.appointmentId,
          appointmentAttendeeId = entity.appointmentInstanceId,
          oicHearingId = null,
          cancelled = true,
          suspended = false,
          categoryCode = entity.categoryCode,
          categoryDescription = "Test Category",
          summary = "Test Category",
          comments = entity.extraInformation,
          prisonerNumber = entity.prisonerNumber,
          inCell = entity.inCell,
          outsidePrison = false,
          date = entity.appointmentDate,
          startTime = entity.startTime,
          endTime = entity.endTime,
          priority = EventType.APPOINTMENT.defaultPriority,
          appointmentSeriesCancellationStartDate = LocalDate.of(2024, 5, 12),
          appointmentSeriesCancellationStartTime = LocalTime.of(10, 20),
          appointmentSeriesFrequency = ModelAppointmentFrequency.WEEKLY,
          paidActivity = null,
          issuePayment = null,
          attendanceStatus = null,
          attendanceReasonCode = null,
        ),
      )
    }

    @Test
    fun `appointment instance without custom name to scheduled event`() {
      val entity = appointmentInstanceEntity(customName = null)
      val scheduledEvents = transform(entity)

      with(scheduledEvents.first()) {
        assertThat(summary).isEqualTo("Test Category")
      }
    }

    @Test
    fun `appointment instance with empty custom name to scheduled event`() {
      val entity = appointmentInstanceEntity(customName = "")
      val scheduledEvents = transform(entity)

      with(scheduledEvents.first()) {
        assertThat(summary).isEqualTo("Test Category")
      }
    }

    @Test
    fun `appointment instance with unknown appointment category to scheduled event`() {
      val entity = appointmentInstanceEntity(categoryCode = "UNKNOWN")
      val scheduledEvents = transform(entity)

      with(scheduledEvents.first()) {
        assertThat(summary).isEqualTo("UNKNOWN")
      }
    }

    @Test
    fun `appointment instance with custom name to scheduled event`() {
      val entity = appointmentInstanceEntity(customName = "Description of appointment")
      val scheduledEvents = transform(entity)

      with(scheduledEvents.first()) {
        assertThat(summary).isEqualTo("Description of appointment (Test Category)")
      }
    }

    @Test
    fun `appointment instance with custom name and unknown category code to scheduled event`() {
      val entity = appointmentInstanceEntity(
        customName = "Description of appointment",
        categoryCode = "UNKNOWN",
      )
      val scheduledEvents = transform(entity)

      with(scheduledEvents.first()) {
        assertThat(summary).isEqualTo("Description of appointment (UNKNOWN)")
      }
    }

    private fun defaultLocationMap(locationId: Long) = mapOf(
      locationId to locationDetails(
        locationId = locationId,
        dpsLocationId = UUID.fromString("999998b5-fbe4-43ae-89c4-cadbc299dd98"),
        code = "LOC123",
        description = "An appointment location",
        agencyId = "TPR",
      ),
    )

    private fun transform(appointmentInstance: AppointmentInstance, locationMap: Map<Long, LocationDetails>? = null): List<ScheduledEvent> {
      val eventPriorities = EventPriorities(EventType.entries.associateWith { listOf(Priority(it.defaultPriority)) })
      val result = appointmentSearchEntity()

      return transformAppointmentInstanceToScheduledEvents(
        "TPR",
        eventPriorities,
        mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)),
        locationMap ?: defaultLocationMap(result.internalLocationId!!),
        listOf(appointmentInstance),
      )
    }
  }

  @Test
  fun `transformation of PrisonerNonAssociation to NonAssociationDetails`() {
    val nonAssociationDetail = PrisonerNonAssociation(
      id = 1,
      role = PrisonerNonAssociation.Role.VICTIM,
      roleDescription = "Victim",
      reason = PrisonerNonAssociation.Reason.BULLYING,
      reasonDescription = "Bullying",
      restrictionType = PrisonerNonAssociation.RestrictionType.LANDING,
      restrictionTypeDescription = "Landing",
      comment = "Bullying",
      authorisedBy = "ADMIN",
      whenCreated = "2022-04-02T00:00:00",
      whenUpdated = "2022-04-02T00:00:00",
      updatedBy = "ADMIN",
      isClosed = false,
      isOpen = true,
      otherPrisonerDetails = uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.OtherPrisonerDetails(
        prisonerNumber = "A1234AA",
        role = uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.nonassociationsapi.model.OtherPrisonerDetails.Role.PERPETRATOR,
        roleDescription = "Perpetrator",
        firstName = "Joseph",
        lastName = "Bloggs",
        prisonId = "MDI",
        prisonName = "HMP Moorland",
      ),
    )

    assertThat(nonAssociationDetail.toModel(true)).isEqualTo(
      NonAssociationDetails(
        allocated = true,
        reasonCode = "BULLYING",
        reasonDescription = "Bullying",
        roleCode = "VICTIM",
        roleDescription = "Victim",
        restrictionType = "LANDING",
        restrictionTypeDescription = "Landing",
        otherPrisonerDetails = OtherPrisonerDetails(
          prisonerNumber = "A1234AA",
          firstName = "Joseph",
          lastName = "Bloggs",
        ),
        whenUpdated = LocalDateTime.parse(nonAssociationDetail.whenUpdated),
        comments = nonAssociationDetail.comment,
      ),
    )
  }

  @Test
  fun `reference code to appointment category summary returns category code for null reference codes`() {
    assertThat((null as ReferenceCode?).toAppointmentCategorySummary("MEDO")).isEqualTo(
      AppointmentCategorySummary("MEDO", "MEDO"),
    )
  }

  @Test
  fun `reference code to appointment category summary mapping`() {
    assertThat(
      appointmentCategoryReferenceCode(
        "MEDO",
        "Medical - Doctor",
      ).toAppointmentCategorySummary("MEDO"),
    ).isEqualTo(
      AppointmentCategorySummary("MEDO", "Medical - Doctor"),
    )
  }

  @Test
  fun `reference code list to appointment category summary list mapping`() {
    assertThat(
      listOf(
        appointmentCategoryReferenceCode(
          "MEDO",
          "Medical - Doctor",
        ),
      ).toAppointmentCategorySummary(),
    ).isEqualTo(
      listOf(AppointmentCategorySummary("MEDO", "Medical - Doctor")),
    )
  }

  @Test
  fun `reference code to appointment name mapping`() {
    assertThat(
      appointmentCategoryReferenceCode("MEDO", "Medical - Doctor")
        .toAppointmentName("MEDO", "John's doctor appointment"),
    ).isEqualTo("John's doctor appointment (Medical - Doctor)")
  }

  @Test
  fun `reference code to appointment name mapping for null reference code`() {
    assertThat(null.toAppointmentName("MEDO", "John's doctor appointment"))
      .isEqualTo("John's doctor appointment (MEDO)")
  }

  @Test
  fun `reference code to appointment name mapping with no description`() {
    assertThat(
      appointmentCategoryReferenceCode("MEDO", "Medical - Doctor")
        .toAppointmentName("MEDO", null),
    ).isEqualTo("Medical - Doctor")
  }

  @Test
  fun `reference code to appointment name mapping for null reference code and no description`() {
    assertThat(null.toAppointmentName("MEDO", null)).isEqualTo("MEDO")
  }

  @Test
  fun `prisoner to summary mapping`() {
    assertThat(
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = 456,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = "TPR",
        cellLocation = "1-2-3",
        category = "A",
      ).toSummary(),
    ).isEqualTo(
      PrisonerSummary(
        "A1234BC",
        456,
        "TEST",
        "PRISONER",
        "ACTIVE IN",
        "TPR",
        "1-2-3",
        "A",
      ),
    )
  }

  @Test
  fun `prisoner to summary mapping defaults`() {
    assertThat(
      PrisonerSearchPrisonerFixture.instance(
        prisonerNumber = "A1234BC",
        bookingId = null,
        firstName = "TEST",
        lastName = "PRISONER",
        prisonId = null,
        cellLocation = null,
      ).toSummary(),
    ).isEqualTo(
      PrisonerSummary(
        "A1234BC",
        -1,
        "TEST",
        "PRISONER",
        "ACTIVE IN",
        "UNKNOWN",
        "UNKNOWN",
        "P",
      ),
    )
  }

  @Test
  fun `prisoner list to summary list mapping`() {
    assertThat(
      listOf(
        PrisonerSearchPrisonerFixture.instance(
          prisonerNumber = "A1234BC",
          bookingId = 456,
          firstName = "TEST",
          lastName = "PRISONER",
          prisonId = "TPR",
          cellLocation = "1-2-3",
          category = "A",
        ),
      ).toSummary(),
    ).isEqualTo(
      listOf(
        PrisonerSummary(
          "A1234BC",
          456,
          "TEST",
          "PRISONER",
          "ACTIVE IN",
          "TPR",
          "1-2-3",
          "A",
        ),
      ),
    )
  }

  @Test
  fun `waiting list entity to waiting list model`() {
    val schedule = activityEntity(prisonCode = PENTONVILLE_PRISON_CODE).schedules().first()
    val allocation = schedule.allocations().first()

    val earliestReleaseDate = earliestReleaseDate()

    with(
      WaitingList(
        waitingListId = 99,
        prisonCode = PENTONVILLE_PRISON_CODE,
        activitySchedule = schedule,
        prisonerNumber = "123456",
        bookingId = 100L,
        applicationDate = TimeSource.today(),
        requestedBy = "Fred",
        comments = "Some random test comments",
        createdBy = "Bob",
        initialStatus = WaitingListStatus.DECLINED,
      ).apply {
        this.allocation = allocation
        this.updatedBy = "Test"
        this.updatedTime = TimeSource.now()
        this.declinedReason = "Needs to attend level one activity first"
      }.toModel(earliestReleaseDate),
    ) {
      id isEqualTo 99
      scheduleId isEqualTo schedule.activityScheduleId
      allocationId isEqualTo schedule.allocations().first().allocationId
      prisonCode isEqualTo DEFAULT_CASELOAD_PENTONVILLE
      prisonerNumber isEqualTo "123456"
      bookingId isEqualTo 100L
      status isEqualTo WaitingListStatus.DECLINED
      statusUpdatedTime isEqualTo null
      requestedDate isEqualTo TimeSource.today()
      requestedBy isEqualTo "Fred"
      comments isEqualTo "Some random test comments"
      createdBy isEqualTo "Bob"
      creationTime isCloseTo TimeSource.now()
      updatedBy isEqualTo "Test"
      updatedTime!! isCloseTo TimeSource.now()
      declinedReason isEqualTo "Needs to attend level one activity first"
      earliestReleaseDate isEqualTo earliestReleaseDate
    }
  }

  @Test
  fun `attendance summary entity to attendance summary model`() {
    val attendanceSummary = ScheduledInstanceAttendanceSummary(
      scheduledInstanceId = 1,
      activityId = 1,
      activityScheduleId = 2,
      prisonCode = "MDI",
      summary = "English 1",
      activityCategoryId = 4,
      sessionDate = LocalDate.now(),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(12, 0),
      inCell = true,
      onWing = false,
      offWing = false,
      cancelled = false,
      attendanceRequired = true,
      allocations = 3,
      attendees = 3,
      notRecorded = 1,
      attended = 1,
      absences = 1,
      paid = 1,
      timeSlot = TimeSlot.AM,
    )

    with(attendanceSummary.toModel()) {
      scheduledInstanceId isEqualTo 1
      activityId isEqualTo 1
      activityScheduleId isEqualTo 2
      summary isEqualTo "English 1"
      categoryId isEqualTo 4
      sessionDate isEqualTo LocalDate.now()
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(12, 0)
      inCell isEqualTo true
      onWing isEqualTo false
      offWing isEqualTo false
      cancelled isEqualTo false
      with(attendanceSummary) {
        allocations isEqualTo 3
        attendees isEqualTo 3
        notRecorded isEqualTo 1
        attended isEqualTo 1
        absences isEqualTo 1
        paid isEqualTo 1
      }
    }
  }

  @Test
  fun `Advance attendance entity to model with history`() {
    val entity = advanceAttendance()

    val model = transform(entity, true, 123)

    with(model) {
      id isEqualTo entity.advanceAttendanceId
      scheduleInstanceId isEqualTo entity.scheduledInstance.scheduledInstanceId
      prisonerNumber isEqualTo entity.prisonerNumber
      issuePayment isEqualTo entity.issuePayment
      payAmount isEqualTo 123
      recordedTime isEqualTo entity.recordedTime
      recordedBy isEqualTo entity.recordedBy
      attendanceHistory!!.size isEqualTo 2

      val historyEntity1 = entity.history().first()
      val historyEntity2 = entity.history().last()

      with(attendanceHistory.first()) {
        id isEqualTo historyEntity2.advanceAttendanceHistoryId
        issuePayment isEqualTo historyEntity2.issuePayment
        recordedBy isEqualTo historyEntity2.recordedBy
        recordedTime isEqualTo historyEntity2.recordedTime
      }

      with(attendanceHistory.last()) {
        id isEqualTo historyEntity1.advanceAttendanceHistoryId
        issuePayment isEqualTo historyEntity1.issuePayment
        recordedBy isEqualTo historyEntity1.recordedBy
        recordedTime isEqualTo historyEntity1.recordedTime
      }
    }
  }

  @Test
  fun `Advance attendance entity to model without history`() {
    val entity = advanceAttendance()

    val model = transform(entity, false, 123)

    with(model) {
      id isEqualTo entity.advanceAttendanceId
      scheduleInstanceId isEqualTo entity.scheduledInstance.scheduledInstanceId
      prisonerNumber isEqualTo entity.prisonerNumber
      issuePayment isEqualTo entity.issuePayment
      payAmount isEqualTo 123
      recordedTime isEqualTo entity.recordedTime
      recordedBy isEqualTo entity.recordedBy
      attendanceHistory isEqualTo null
    }
  }

  @Test
  fun `Convert to money format`() {
    val amount = toMoney(100)
    amount isEqualTo "£1.00"
  }

  @Test
  fun `Convert to money format with zero`() {
    val amount = toMoney(0)
    amount isEqualTo "£0.00"
  }

  @Test
  fun `Convert to money format with decimal`() {
    val amount = toMoney(59)
    amount isEqualTo "£0.59"
  }
}
