package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.UserDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.activityEntity
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.lowPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.rolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityScheduleSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Allocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.InternalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonPayBand
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.RolloutPrison
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.UserSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonerSearchPrisonerFixture
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityEligibility as ModelActivityEligibility
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityMinimumEducationLevel as ModelActivityMinimumEducationLevel
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityPay as ModelActivityPay
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivitySchedule as ModelActivitySchedule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ActivityTier as ModelActivityTier
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.Attendance as ModelAttendance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.EligibilityRule as ModelEligibilityRule
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.PrisonerWaiting as ModelActivityWaiting
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.ScheduledInstance as ModelScheduledInstance
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.model.response.ActivityCategory as ModelActivityCategory

class TransformFunctionsTest {

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
          -1,
          ModelEligibilityRule(1, code = "OVER_21", description = "The prisoner must be over 21 to attend"),
        ),
      )
      assertThat(schedules).containsExactly(
        ModelActivitySchedule(
          id = 1,
          instances = listOf(
            ModelScheduledInstance(
              id = -1,
              date = timestamp.toLocalDate(),
              startTime = timestamp.toLocalTime(),
              endTime = timestamp.toLocalTime().plusHours(1),
              cancelled = false,
              attendances = listOf(
                ModelAttendance(
                  id = 1,
                  scheduleInstanceId = -1,
                  prisonerNumber = "A1234AA",
                  status = "WAITING",
                  issuePayment = null,
                  incentiveLevelWarningIssued = null,
                  recordedTime = LocalDate.now().atStartOfDay(),
                  recordedBy = "Joe Bloggs",
                ),
              ),
            ),
          ),
          internalLocation = InternalLocation(1, "EDU-ROOM-1", "Education - R1"),
          allocations = listOf(
            Allocation(
              id = -1,
              prisonerNumber = "A1234AA",
              bookingId = 10001,
              prisonPayBand = PrisonPayBand(
                id = lowPayBand.prisonPayBandId,
                displaySequence = lowPayBand.displaySequence,
                alias = lowPayBand.payBandAlias,
                description = lowPayBand.payBandDescription,
                prisonCode = lowPayBand.prisonCode,
                nomisPayBand = lowPayBand.nomisPayBand,
              ),
              startDate = timestamp.toLocalDate(),
              endDate = null,
              allocatedTime = timestamp,
              allocatedBy = "Mr Blogs",
              activitySummary = "Maths",
              scheduleId = 1,
              scheduleDescription = "schedule description",
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
          id = -1,
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
          id = -1,
          educationLevelCode = "1",
          educationLevelDescription = "Reading Measure 1.0",
        ),
      )
    }
  }

  @Test
  fun `transformation of rollout prison entity to rollout prison model`() {
    assertThat(transform(rolloutPrison())).isEqualTo(
      RolloutPrison(
        1,
        "PVI",
        "HMP Pentonville",
        true,
        rolloutDate = LocalDate.of(2022, 12, 22),
        isAppointmentsEnabled = false,
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
  fun `location to appointment location summary returns unknown for null locations`() {
    assertThat((null as Location?).toAppointmentLocationSummary(1, "TPR")).isEqualTo(
      AppointmentLocationSummary(1, "TPR", "UNKNOWN"),
    )
  }

  @Test
  fun `location to appointment location summary mapping`() {
    assertThat(appointmentLocation(1, "TPR").toAppointmentLocationSummary(1, "TPR")).isEqualTo(
      AppointmentLocationSummary(1, "TPR", "Test Appointment Location"),
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
