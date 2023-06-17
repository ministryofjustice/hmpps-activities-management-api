package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.OffenderNonAssociationDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.rangeTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.userDetail
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.PrisonApiMockServer
import java.time.LocalDate
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.typeOf

class PrisonApiClientTest {

  private lateinit var prisonApiClient: PrisonApiClient

  companion object {
    @JvmField
    internal val prisonApiMockServer = PrisonApiMockServer()

    @BeforeAll
    @JvmStatic
    fun startMocks() {
      prisonApiMockServer.start()
    }

    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonApiMockServer.stop()
    }
  }

  @BeforeEach
  fun resetStubs() {
    prisonApiMockServer.resetAll()
    val webClient = WebClient.create("http://localhost:${prisonApiMockServer.port()}")
    prisonApiClient = PrisonApiClient(webClient)
  }

  @Test
  fun `getPrisonerDetails - success`() {
    val prisonerNumber = "G4793VF"

    prisonApiMockServer.stubGetPrisonerDetails(prisonerNumber)

    val prisonerDetails = prisonApiClient.getPrisonerDetails(prisonerNumber).block()!!
    assertThat(prisonerDetails.bookingId).isEqualTo(1200993)
  }

  @Test
  fun `getPrisonerDetails - not found`() {
    val prisonerNumber = "AAAAA"

    prisonApiMockServer.stubGetPrisonerDetailsNotFound(prisonerNumber)

    assertThatThrownBy { prisonApiClient.getPrisonerDetails(prisonerNumber).block() }
      .isInstanceOf(WebClientResponseException::class.java)
      .hasMessage("404 Not Found from GET http://localhost:8999/api/bookings/offenderNo/AAAAA?fullInfo=true")
  }

  @Test
  fun `getScheduledAppointmentsAsync by booking id - success`(): Unit = runBlocking {
    val bookingId = 10001L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))

    prisonApiMockServer.stubGetScheduledAppointments(bookingId, dateRange.start, dateRange.endInclusive)

    val scheduledAppointments = prisonApiClient.getScheduledAppointmentsAsync(bookingId, dateRange)
    assertThat(scheduledAppointments).hasSize(1)
    assertThat(scheduledAppointments.first().bookingId).isEqualTo(10001L)
  }

  @Test
  fun `getScheduledAppointmentsAsync by booking id - not found`(): Unit = runBlocking {
    val bookingId = 0L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))

    prisonApiMockServer.stubGetScheduledAppointmentsNotFound(bookingId, dateRange.start, dateRange.endInclusive)

    assertThrows<WebClientResponseException>(
      "404 Not Found from GET http://localhost:8999/api/bookings/0/appointments?fromDate=2022-10-01&toDate=2022-11-05",
    ) { prisonApiClient.getScheduledAppointmentsAsync(bookingId, dateRange) }
  }

  @Test
  fun `getScheduledAppointmentsForPrisonerNumbersAsync - success`(): Unit = runBlocking {
    val prisonCode = "MDI"
    val prisonerNumbers = setOf("G4793VF")
    val date = LocalDate.of(2022, 12, 14)

    prisonApiMockServer.stubGetScheduledAppointmentsForPrisonerNumbers(prisonCode, date)

    val scheduledAppointments =
      prisonApiClient.getScheduledAppointmentsForPrisonerNumbersAsync(prisonCode, prisonerNumbers, date, null)
    assertThat(scheduledAppointments).hasSize(2)
    assertThat(scheduledAppointments.first().offenderNo).isEqualTo("A5193DY")
  }

  @Test
  fun `getScheduledActivitiesAsync - success`(): Unit = runBlocking {
    val bookingId = 10001L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))

    prisonApiMockServer.stubGetScheduledActivities(bookingId, dateRange.start, dateRange.endInclusive)

    val scheduledActivities = prisonApiClient.getScheduledActivitiesAsync(bookingId, dateRange)
    assertThat(scheduledActivities).hasSize(2)
    assertThat(scheduledActivities.first().bookingId).isEqualTo(10001L)
  }

  @Test
  fun `getScheduledActivitiesAsync - not found`(): Unit = runBlocking {
    val bookingId = 0L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))

    prisonApiMockServer.stubGetScheduledActivitiesNotFound(bookingId, dateRange.start, dateRange.endInclusive)

    assertThrows<WebClientResponseException>(
      "404 Not Found from GET http://localhost:8999/api/bookings/0/activities?fromDate=2022-10-01&toDate=2022-11-05",
    ) { prisonApiClient.getScheduledActivitiesAsync(bookingId, dateRange) }
  }

  @Test
  fun `getScheduledActivitiesForPrisonerNumbersAsync - success`(): Unit = runBlocking {
    val prisonCode = "MDI"
    val prisonerNumbers = setOf("G4793VF", "A5193DY")
    val date = LocalDate.of(2022, 12, 14)

    prisonApiMockServer.stubGetScheduledActivitiesForPrisonerNumbers(prisonCode, date)

    val activities =
      prisonApiClient.getScheduledActivitiesForPrisonerNumbersAsync(prisonCode, prisonerNumbers, date, null)
    assertThat(activities).hasSize(2)
    assertThat(activities.first().offenderNo).isIn("G4793VF", "A5193DY")
  }

  @Test
  fun `getCourtHearingsAsync by booking id - success`(): Unit = runBlocking {
    val bookingId = 10001L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))

    prisonApiMockServer.stubGetCourtHearings(bookingId, dateRange.start, dateRange.endInclusive)

    val courtHearings = prisonApiClient.getScheduledCourtHearingsAsync(bookingId, dateRange)
    assertThat(courtHearings.hearings).hasSize(4)
    assertThat(courtHearings.hearings?.first()?.id).isEqualTo(1L)
  }

  @Test
  fun `getCourtHearingsAsync by booking id - not found`(): Unit = runBlocking {
    val bookingId = 0L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))

    prisonApiMockServer.stubGetCourtHearingsNotFound(bookingId, dateRange.start, dateRange.endInclusive)

    assertThrows<WebClientResponseException>(
      "404 Not Found from GET http://localhost:8999/api/bookings/0/court-hearings?fromDate=2022-10-01&toDate=2022-11-05",
    ) { prisonApiClient.getScheduledCourtHearingsAsync(bookingId, dateRange) }
  }

  @Test
  fun `getCourtEventsForPrisonerNumbersAsync - success`(): Unit = runBlocking {
    val prisonCode = "MDI"
    val prisonerNumbers = setOf("G4793VF")
    val date = LocalDate.of(2022, 12, 14)

    prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)

    val courtEvents =
      prisonApiClient.getScheduledCourtEventsForPrisonerNumbersAsync(prisonCode, prisonerNumbers, date, null)
    assertThat(courtEvents).hasSize(2)
    assertThat(courtEvents.first().offenderNo).isEqualTo("G4793VF")
  }

  @Test
  fun `getScheduledVisitsAsync for booking id - success`(): Unit = runBlocking {
    val bookingId = 10002L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))

    prisonApiMockServer.stubGetScheduledVisits(bookingId, dateRange.start, dateRange.endInclusive)

    val scheduledVisits = prisonApiClient.getScheduledVisitsAsync(bookingId, dateRange)
    assertThat(scheduledVisits).hasSize(1)
    assertThat(scheduledVisits.first().bookingId).isEqualTo(10002L)
  }

  @Test
  fun `getScheduledVisitsAsync for booking id - not found`(): Unit = runBlocking {
    val bookingId = 0L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))

    prisonApiMockServer.stubGetScheduledVisitsNotFound(bookingId, dateRange.start, dateRange.endInclusive)

    assertThrows<WebClientResponseException>(
      "404 Not Found from GET http://localhost:8999/api/bookings/0/visits?fromDate=2022-10-01&toDate=2022-11-05",
    ) { prisonApiClient.getScheduledVisitsAsync(bookingId, dateRange) }
  }

  @Test
  fun `getScheduledVisitsForPrisonerNumbersAsync - success`(): Unit = runBlocking {
    val prisonCode = "MDI"
    val prisonerNumbers = setOf("A5193DY")
    val date = LocalDate.of(2022, 12, 14)

    prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)

    val visits = prisonApiClient.getScheduledVisitsForPrisonerNumbersAsync(prisonCode, prisonerNumbers, date, null)
    assertThat(visits).hasSize(2)
    assertThat(visits.first().offenderNo).isEqualTo("A5193DY")
  }

  @Test
  fun `getLocationsForType - success`() {
    val agencyId = "LEI"
    val locationType = "CELL"
    prisonApiMockServer.stubGetLocationsForType(agencyId, locationType, "prisonapi/locations-LEI-HB7.json")
    val locations = prisonApiClient.getLocationsForType(agencyId, locationType).block()!!
    assertThat(locations).hasSize(4)
    assertThat(locations[3].locationId).isEqualTo(108583L)
  }

  @Test
  fun `getLocationsForType - not found`() {
    val agencyId = "LEI"
    val locationType = "CELL"
    prisonApiMockServer.stubGetLocationsForTypeNotFound(agencyId, locationType)
    assertThatThrownBy { prisonApiClient.getLocationsForType(agencyId, locationType).block() }
      .isInstanceOf(WebClientResponseException::class.java)
      .hasMessage("404 Not Found from GET http://localhost:8999/api/agencies/LEI/locations/type/CELL")
  }

  @Test
  fun `getLocationsForTypeUnrestricted - success`() {
    val agencyId = "LEI"
    val locationType = "CELL"
    prisonApiMockServer.stubGetLocationsForTypeUnrestricted(agencyId, locationType, "prisonapi/locations-LEI-HB7.json")
    val locations = prisonApiClient.getLocationsForTypeUnrestricted(agencyId, locationType).block()!!
    assertThat(locations).hasSize(4)
    assertThat(locations[3].locationId).isEqualTo(108583L)
  }

  @Test
  fun `getLocationsForTypeUnrestricted - not found`() {
    val agencyId = "LEI"
    val locationType = "CELL"
    prisonApiMockServer.stubGetLocationsForTypeUnrestrictedNotFound(agencyId, locationType)
    assertThatThrownBy { prisonApiClient.getLocationsForTypeUnrestricted(agencyId, locationType).block() }
      .isInstanceOf(WebClientResponseException::class.java)
      .hasMessage("404 Not Found from GET http://localhost:8999/api/agencies/LEI/locations?eventType=CELL")
  }

  @Test
  fun `getLocationGroups - success`() {
    val agencyId = "MDI"
    prisonApiMockServer.stubGetLocationGroups(agencyId, "prisonapi/location-groups-1.json")
    val locationGroups = prisonApiClient.getLocationGroups(agencyId).block()!!
    assertThat(locationGroups).hasSize(1)
    assertThat(locationGroups.first().children.first().name).isEqualTo("Child Group Name")
  }

  @Test
  fun `getLocationGroups - not found`() {
    val agencyId = "LEI"
    prisonApiMockServer.stubGetLocationGroupsNotFound(agencyId)
    assertThatThrownBy { prisonApiClient.getLocationGroups(agencyId).block() }
      .isInstanceOf(WebClientResponseException::class.java)
      .hasMessage("404 Not Found from GET http://localhost:8999/api/agencies/LEI/locations/groups")
  }

  @Test
  fun `getLocation - success`() {
    prisonApiMockServer.stubGetLocation(1, "prisonapi/location-id-1.json")

    assertThat(prisonApiClient.getLocation(1L).block()!!).isEqualTo(
      Location(
        locationId = 1,
        locationType = "CELL",
        description = "House_block_7-1-002",
        agencyId = "MDI",
        currentOccupancy = 1,
        locationPrefix = "LEI-House-block-7-1-002",
        operationalCapacity = 2,
        userDescription = "user description",
        internalLocationCode = "internal location code",
      ),
    )
  }

  @Test
  fun `getStudyArea - success`() {
    prisonApiMockServer.stubGetReferenceCode("STUDY_AREA", "ENGLA", "prisonapi/study-area-code-ENGLA.json")

    assertThat(prisonApiClient.getStudyArea("ENGLA").block()!!).isEqualTo(
      ReferenceCode(
        domain = "STUDY_AREA",
        code = "ENGLA",
        description = "English Language",
        activeFlag = "Y",
        listSeq = 99,
        systemDataFlag = "N",
      ),
    )
  }

  @Test
  fun `getEducationLevel - success`() {
    prisonApiMockServer.stubGetReferenceCode("EDU_LEVEL", "1", "prisonapi/education-level-code-1.json")

    assertThat(prisonApiClient.getEducationLevel("1").block()!!).isEqualTo(
      ReferenceCode(
        domain = "EDU_LEVEL",
        code = "1",
        description = "Reading Measure 1.0",
        parentCode = "STL",
        activeFlag = "Y",
        listSeq = 6,
        systemDataFlag = "N",
      ),
    )
  }

  @Test
  fun `getUserDetailsList - success`() {
    prisonApiMockServer.stubGetUserDetailsList(listOf("TEST.USER"))

    assertThat(prisonApiClient.getUserDetailsList(listOf("TEST.USER"))).isEqualTo(
      listOf(
        userDetail(1, "TEST.USER", "TEST1", "USER1"),
      ),
    )
  }

  @Test
  fun `getExternalTransfersOnDateAsync - success`(): Unit = runBlocking {
    val prisonCode = "MDI"
    val prisonerNumbers = setOf("B4793VX")
    val date = LocalDate.now()

    prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers, date)

    val externalTransfers = prisonApiClient.getExternalTransfersOnDateAsync(prisonCode, prisonerNumbers, date)
    assertThat(externalTransfers).hasSize(1)
    assertThat(externalTransfers.first().offenderNo).isEqualTo("B4793VX")
  }

  @Test
  fun `getReferenceCodes - success`() {
    prisonApiMockServer.stubGetAppointmentCategoryReferenceCodes()

    assertThat(prisonApiClient.getReferenceCodes("INT_SCH_RSN")).isEqualTo(
      listOf(
        appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
        appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
        appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
      ),
    )
  }

  @Test
  fun `getScheduleReasons - success`() {
    prisonApiMockServer.stubGetAppointmentScheduleReasons()

    assertThat(prisonApiClient.getScheduleReasons("APP")).isEqualTo(
      listOf(
        appointmentCategoryReferenceCode("AC1", "Appointment Category 1"),
        appointmentCategoryReferenceCode("AC2", "Appointment Category 2"),
        appointmentCategoryReferenceCode("AC3", "Appointment Category 3"),
      ),
    )
  }

  @Test
  fun `getOffenderAdjudications - success`(): Unit = runBlocking {
    val prisonCode = "MDI"
    val prisonerNumbers = listOf("B4793VX")
    val fromDate = LocalDate.now()
    val toDate = fromDate.plusDays(1)

    prisonApiMockServer.stubAdjudicationHearing(prisonCode, fromDate.rangeTo(toDate), prisonerNumbers.toList(), TimeSlot.AM)

    val adjudications = prisonApiClient.getOffenderAdjudications(
      prisonCode,
      fromDate.rangeTo(toDate),
      prisonerNumbers.toSet(),
      TimeSlot.AM,
    )

    assertThat(adjudications).hasSize(1)

    with(adjudications.first()) {
      assertThat(agencyId).isEqualTo("MDI")
      assertThat(offenderNo).isEqualTo("B4793VX")
      assertThat(hearingId).isEqualTo(1)
    }
  }

  @Test
  fun `verify overridden return type for non-associations`() {
    val function = PrisonApiClient::class.declaredFunctions.first { it.name == "getOffenderNonAssociations" }

    assertThat(function.returnType).isEqualTo(typeOf<List<OffenderNonAssociationDetail>?>())
  }
}
