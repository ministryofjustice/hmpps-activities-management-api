package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.OffenderNonAssociationDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.AppointmentInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.EventType
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.entity.PrisonerStatus
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentInstanceEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentOccurrenceSearchEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.NonAssociationDetails
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.suitability.nonassociation.OffenderNonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.EventPriorities
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationService
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.Priority
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ReferenceCodeDomain
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.ReferenceCodeService
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.OffenderNonAssociation as PrisonApiOffenderNonAssociation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityEligibility as ModelActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel as ModelActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier as ModelActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceHistory as ModelAttendanceHistory
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AttendanceReason as ModelAttendanceReason
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EligibilityRule as ModelEligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerWaiting as ModelActivityWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledEvent as ModelScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstance as ModelScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

class TransformFunctionsTest {
  private val referenceCodeService: ReferenceCodeService = mock()
  private val locationService: LocationService = mock()

  @Test
  fun `transformation of activity entity to the activity models`() {
    val timestamp = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
    val activity = activityEntity(timestamp = timestamp).apply { attendanceRequired = false }

    with(transform(activity)) {
      assertThat(id).isEqualTo(1)
      assertThat(prisonCode).isEqualTo("123")
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
      assertThat(tier).isEqualTo(ModelActivityTier(1, "T1", "Tier 1"))
      assertThat(eligibilityRules).containsExactly(
        ModelActivityEligibility(
          0,
          ModelEligibilityRule(1, code = "OVER_21", description = "The prisoner must be over 21 to attend"),
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
                  attendanceHistory = listOf(
                    ModelAttendanceHistory(
                      id = 1,
                      attendanceReason = ModelAttendanceReason(
                        9,
                        "ATTENDED",
                        "Previous Desc",
                        false,
                        true,
                        true,
                        false,
                        false,
                        false,
                        true,
                        1,
                        "some note",
                      ),
                      issuePayment = null,
                      incentiveLevelWarningIssued = null,
                      comment = "previous comment",
                      recordedBy = "Joe Bloggs",
                      recordedTime = LocalDate.now().atStartOfDay(),
                    ),
                  ),
                ),
              ),
            ),
          ),
          internalLocation = InternalLocation(1, "EDU-ROOM-1", "Education - R1"),
          allocations = listOf(
            Allocation(
              id = 0,
              prisonerNumber = "A1234AA",
              bookingId = 10001,
              prisonPayBand = lowPayBand.toModel(),
              startDate = timestamp.toLocalDate(),
              endDate = null,
              allocatedTime = timestamp,
              allocatedBy = "Mr Blogs",
              activitySummary = "Maths",
              scheduleId = 1,
              scheduleDescription = "schedule description",
              status = PrisonerStatus.ACTIVE,
            ),
          ),
          description = "schedule description",
          capacity = 1,
          activity = activity.toModelLite(),
          slots = listOf(
            ActivityScheduleSlot(
              id = 1L,
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
        ),
      )
      assertThat(waitingList).containsExactly(
        ModelActivityWaiting(
          id = 1,
          prisonerNumber = "A1234AA",
          priority = 1,
          createdTime = timestamp,
          createdBy = "test",
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
  @DisplayName("appointmentInstanceToScheduledEvents")
  inner class appointmentInstanceToScheduledEvents {
    @Test
    fun `appointment instance to scheduled event`() {
      val entity = appointmentInstanceEntity()
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
          internalLocationDescription = "User location desc",
          eventId = null,
          appointmentId = entity.appointmentId,
          appointmentOccurrenceId = entity.appointmentOccurrenceId,
          appointmentInstanceId = entity.appointmentInstanceId,
          appointmentDescription = entity.appointmentDescription,
          oicHearingId = null,
          cancelled = entity.isCancelled,
          suspended = false,
          categoryCode = entity.categoryCode,
          categoryDescription = "Test Category",
          summary = "Test Category",
          comments = entity.comment,
          prisonerNumber = entity.prisonerNumber,
          inCell = entity.inCell,
          outsidePrison = false,
          date = entity.appointmentDate,
          startTime = entity.startTime,
          endTime = entity.endTime,
          priority = EventType.APPOINTMENT.defaultPriority,
        ),
      )
    }

    @Test
    fun `appointment instance without appointmentDescription to scheduled event`() {
      val entity = appointmentInstanceEntity(appointmentDescription = null)
      val scheduledEvents = transform(entity)

      with(scheduledEvents.first()) {
        assertThat(summary).isEqualTo("Test Category")
      }
    }

    @Test
    fun `appointment instance with empty appointmentDescription to scheduled event`() {
      val entity = appointmentInstanceEntity(appointmentDescription = "")
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
    fun `appointment instance with appointmentDescription to scheduled event`() {
      val entity = appointmentInstanceEntity(appointmentDescription = "Description of appointment")
      val scheduledEvents = transform(entity)

      with(scheduledEvents.first()) {
        assertThat(summary).isEqualTo("Description of appointment (Test Category)")
      }
    }

    @Test
    fun `appointment instance with appointmentDescription and unknown category code to scheduled event`() {
      val entity = appointmentInstanceEntity(
        appointmentDescription = "Description of appointment",
        categoryCode = "UNKNOWN",
      )
      val scheduledEvents = transform(entity)

      with(scheduledEvents.first()) {
        assertThat(summary).isEqualTo("Description of appointment (UNKNOWN)")
      }
    }

    private fun transform(appointmentInstance: AppointmentInstance): List<ScheduledEvent> {
      val eventPriorities = EventPriorities(EventType.values().associateWith { listOf(Priority(it.defaultPriority)) })
      val result = appointmentOccurrenceSearchEntity()

      whenever(referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY))
        .thenReturn(mapOf(result.categoryCode to appointmentCategoryReferenceCode(result.categoryCode)))
      whenever(locationService.getLocationsForAppointmentsMap(result.prisonCode))
        .thenReturn(
          mapOf(
            result.internalLocationId!! to Location(
              locationId = 1,
              internalLocationCode = "LOC123",
              description = "An appointment location",
              userDescription = "User location desc",
              locationType = "APP",
              agencyId = "TPR",
            ),
          ),
        )

      val referenceCodesForAppointmentsMap =
        referenceCodeService.getReferenceCodesMap(ReferenceCodeDomain.APPOINTMENT_CATEGORY)
      val locationsForAppointmentsMap = locationService.getLocationsForAppointmentsMap(result.prisonCode)

      return transformAppointmentInstanceToScheduledEvents(
        "TPR",
        eventPriorities,
        referenceCodesForAppointmentsMap,
        locationsForAppointmentsMap,
        listOf(appointmentInstance),
      )
    }
  }

  @Test
  fun `transformation of rollout prison entity to rollout prison model`() {
    assertThat(transform(rolloutPrison())).isEqualTo(
      RolloutPrisonPlan(
        prisonCode = "PVI",
        activitiesRolledOut = true,
        activitiesRolloutDate = LocalDate.of(2022, 12, 22),
        appointmentsRolledOut = true,
        appointmentsRolloutDate = LocalDate.of(2022, 12, 23),
      ),
    )
  }

  @Test
  fun `transformation of OffenderNonAssociationDetail to NonAssociationDetails`() {
    val nonAssociationDetail = OffenderNonAssociationDetail(
      reasonCode = "VIC",
      reasonDescription = "Victim",
      typeCode = "WING",
      typeDescription = "Do Not Locate on Same Wing",
      effectiveDate = "2021-07-05T10:35:17",
      expiryDate = "2024-07-05T10:35:17",
      offenderNonAssociation = PrisonApiOffenderNonAssociation(
        offenderNo = "G0135GA",
        firstName = "Joseph",
        lastName = "Bloggs",
        reasonCode = "PER",
        reasonDescription = "Perpetrator",
        agencyDescription = "Pentonville",
        assignedLivingUnitDescription = "PVI-1-2-4",
        assignedLivingUnitId = 123,
      ),
      authorisedBy = "Test user",
      comments = "A comment",
    )

    assertThat(transformOffenderNonAssociationDetail(nonAssociationDetail)).isEqualTo(
      NonAssociationDetails(
        reasonCode = "VIC",
        reasonDescription = "Victim",
        typeCode = "WING",
        typeDescription = "Do Not Locate on Same Wing",
        effectiveDate = LocalDateTime.parse("2021-07-05T10:35:17"),
        expiryDate = LocalDateTime.parse("2024-07-05T10:35:17"),
        offenderNonAssociation = OffenderNonAssociation(
          offenderNo = "G0135GA",
          firstName = "Joseph",
          lastName = "Bloggs",
          reasonCode = "PER",
          reasonDescription = "Perpetrator",
          agencyDescription = "Pentonville",
          assignedLivingUnitDescription = "PVI-1-2-4",
          assignedLivingUnitId = 123,
        ),
        authorisedBy = "Test user",
        comments = "A comment",
      ),
    )
  }

  @Test
  fun `reference code to appointment category summary returns unknown for null reference codes`() {
    assertThat((null as ReferenceCode?).toAppointmentCategorySummary("MEDO")).isEqualTo(
      AppointmentCategorySummary("MEDO", "UNKNOWN"),
    )
  }

  @Test
  fun `reference code to appointment category summary mapping`() {
    assertThat(appointmentCategoryReferenceCode("MEDO", "Medical - Doctor").toAppointmentCategorySummary("MEDO")).isEqualTo(
      AppointmentCategorySummary("MEDO", "Medical - Doctor"),
    )
  }

  @Test
  fun `reference code list to appointment category summary list mapping`() {
    assertThat(listOf(appointmentCategoryReferenceCode("MEDO", "Medical - Doctor")).toAppointmentCategorySummary()).isEqualTo(
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
      .isEqualTo("John's doctor appointment (UNKNOWN)")
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
    assertThat(null.toAppointmentName("MEDO", null)).isEqualTo("UNKNOWN")
  }

  @Test
  fun `location to appointment location summary returns unknown for null locations`() {
    assertThat((null as Location?).toAppointmentLocationSummary(1, "TPR")).isEqualTo(
      AppointmentLocationSummary(1, "TPR", "UNKNOWN"),
    )
  }

  @Test
  fun `location to appointment location summary mapping`() {
    assertThat(appointmentLocation(1, "TPR").toAppointmentLocationSummary(1, "TPR")).isEqualTo(
      AppointmentLocationSummary(1, "TPR", "Test Appointment Location User Description"),
    )
  }

  @Test
  fun `user detail to summary returns unknown for null user details`() {
    assertThat((null as UserDetail?).toSummary("TEST.USER")).isEqualTo(
      UserSummary(-1, "TEST.USER", "UNKNOWN", "UNKNOWN"),
    )
  }

  @Test
  fun `user detail to summary mapping`() {
    assertThat(userDetail(1, "TEST.USER", "TEST", "USER").toSummary("TEST.USER")).isEqualTo(
      UserSummary(1, "TEST.USER", "TEST", "USER"),
    )
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
      ).toSummary(),
    ).isEqualTo(
      PrisonerSummary(
        "A1234BC",
        456,
        "TEST",
        "PRISONER",
        "TPR",
        "1-2-3",
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
        "UNKNOWN",
        "UNKNOWN",
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
        ),
      ).toSummary(),
    ).isEqualTo(
      listOf(
        PrisonerSummary(
          "A1234BC",
          456,
          "TEST",
          "PRISONER",
          "TPR",
          "1-2-3",
        ),
      ),
    )
  }
}
