package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.api

import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.LocationSummary
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.Movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.prisonapi.overrides.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.common.LocalDateRange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.MOORLAND_PRISON_CODE
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentCategoryReferenceCode
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.appointmentLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.containsExactly
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.internalLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.movement
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.PrisonApiPrisonerScheduleFixture
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

    val prisonerDetails = prisonApiClient.getPrisonerDetailsLite(prisonerNumber)
    assertThat(prisonerDetails.bookingId).isEqualTo(1200993)
  }

  @Test
  fun `getPrisonerDetails - not found`() {
    val prisonerNumber = "AAAAA"

    prisonApiMockServer.stubGetPrisonerDetailsNotFound(prisonerNumber)

    assertThatThrownBy { prisonApiClient.getPrisonerDetailsLite(prisonerNumber) }
      .isInstanceOf(WebClientResponseException::class.java)
      .hasMessage("404 Not Found from GET http://localhost:8999/api/bookings/offenderNo/AAAAA")
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
  fun `getScheduledAppointmentsForPrisonerNumbersAsync - empty prisoner list shouldn't call API`(): Unit = runBlocking {
    val prisonCode = "MDI"
    val prisonerNumbers = emptySet<String>()
    val date = LocalDate.of(2022, 12, 14)

    fun PrisonApiMockServer.verifyNoClientRequests() =
      verify(0, postRequestedFor(urlEqualTo("/api/schedules/$prisonCode/appointments?date=$date")))

    prisonApiMockServer.stubGetScheduledAppointmentsForPrisonerNumbers(prisonCode, date)

    val scheduledAppointments =
      prisonApiClient.getScheduledAppointmentsForPrisonerNumbersAsync(prisonCode, prisonerNumbers, date, null)
    assertThat(scheduledAppointments).hasSize(0)
    prisonApiMockServer.verifyNoClientRequests()
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

  @Nested
  @DisplayName("Retrying failed api calls")
  inner class RetryFailedCalls {
    val bookingId = 10001L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))

    @Test
    fun `will succeed if number of fails is not less than maximum allowed`(): Unit = runBlocking {
      prisonApiMockServer.stubGetScheduledActivitiesWithConnectionReset(
        bookingId,
        dateRange.start,
        dateRange.endInclusive,
      )

      val scheduledActivities = prisonApiClient.getScheduledActivitiesAsync(bookingId, dateRange)
      assertThat(scheduledActivities).hasSize(2)
      assertThat(scheduledActivities.first().bookingId).isEqualTo(10001L)
    }

    @Test
    fun `will succeed if number of fails is maximum allowed`(): Unit = runBlocking {
      prisonApiMockServer.stubGetScheduledActivitiesWithConnectionReset(
        bookingId,
        dateRange.start,
        dateRange.endInclusive,
        2,
      )

      val scheduledActivities = prisonApiClient.getScheduledActivitiesAsync(bookingId, dateRange)
      assertThat(scheduledActivities).hasSize(2)
      assertThat(scheduledActivities.first().bookingId).isEqualTo(10001L)
    }

    @Test
    fun `will fail if number of fails is more than maximum allowed`(): Unit = runBlocking {
      prisonApiMockServer.stubGetScheduledActivitiesWithConnectionReset(
        bookingId,
        dateRange.start,
        dateRange.endInclusive,
        3,
      )

      assertThrows<WebClientRequestException> {
        prisonApiClient.getScheduledActivitiesAsync(bookingId, dateRange)
      }
    }
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
  fun `getScheduledActivitiesForPrisonerNumbersAsync - empty prisoner list shouldn't call API`(): Unit = runBlocking {
    val prisonCode = "MDI"
    val prisonerNumbers = emptySet<String>()
    val date = LocalDate.of(2022, 12, 14)

    fun PrisonApiMockServer.verifyNoClientRequests() =
      verify(0, postRequestedFor(urlEqualTo("/api/schedules/$prisonCode/activities?date=$date")))

    prisonApiMockServer.stubGetScheduledActivitiesForPrisonerNumbers(prisonCode, date)

    val activities =
      prisonApiClient.getScheduledActivitiesForPrisonerNumbersAsync(prisonCode, prisonerNumbers, date, null)
    assertThat(activities).hasSize(0)
    prisonApiMockServer.verifyNoClientRequests()
  }

  @Test
  fun `getCourtHearingsAsync by booking id - success`(): Unit = runBlocking {
    val bookingId = 10001L
    val dateRange = LocalDateRange(LocalDate.of(2022, 10, 1), LocalDate.of(2022, 11, 5))

    prisonApiMockServer.stubGetCourtHearings(bookingId, dateRange.start, dateRange.endInclusive)

    val courtHearings = prisonApiClient.getScheduledCourtHearingsAsync(bookingId, dateRange)
    assertThat(courtHearings?.hearings).hasSize(4)
    assertThat(courtHearings?.hearings?.first()?.id).isEqualTo(1L)
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
  fun `getCourtEventsForPrisonerNumbersAsync - empty prisoner list shouldn't call API`(): Unit = runBlocking {
    val prisonCode = "MDI"
    val prisonerNumbers = emptySet<String>()
    val date = LocalDate.of(2022, 12, 14)

    fun PrisonApiMockServer.verifyNoClientRequests() =
      verify(0, postRequestedFor(urlEqualTo("/api/schedules/$prisonCode/courtEvents?date=$date")))

    prisonApiMockServer.stubGetCourtEventsForPrisonerNumbers(prisonCode, date)

    val courtEvents =
      prisonApiClient.getScheduledCourtEventsForPrisonerNumbersAsync(prisonCode, prisonerNumbers, date, null)
    assertThat(courtEvents).hasSize(0)
    prisonApiMockServer.verifyNoClientRequests()
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
  fun `getScheduledVisitsForPrisonerNumbersAsync - empty prisoner list shouldn't call API`(): Unit = runBlocking {
    val prisonCode = "MDI"
    val prisonerNumbers = emptySet<String>()
    val date = LocalDate.of(2022, 12, 14)

    fun PrisonApiMockServer.verifyNoClientRequests() =
      verify(0, postRequestedFor(urlEqualTo("/api/schedules/$prisonCode/visits?date=$date")))

    prisonApiMockServer.stubGetScheduledVisitsForPrisonerNumbers(prisonCode, date)

    val visits = prisonApiClient.getScheduledVisitsForPrisonerNumbersAsync(prisonCode, prisonerNumbers, date, null)
    assertThat(visits).hasSize(0)
    prisonApiMockServer.verifyNoClientRequests()
  }

  @Test
  fun `getScheduledVisitsForLocationAsync - success`(): Unit = runBlocking {
    val prisonCode = "MDI"
    val locationId = 1L
    val date = LocalDate.of(2022, 10, 10)
    val visits = listOf(PrisonApiPrisonerScheduleFixture.visitInstance(eventId = 1, locationId = locationId, date = date))

    prisonApiMockServer.stubScheduledVisitsForLocation(prisonCode, locationId, date, null, visits)

    runBlocking {
      prisonApiClient.getScheduledVisitsForLocationAsync(prisonCode, locationId, date, null) isEqualTo visits
    }
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

    assertThrows<WebClientResponseException>(
      "404 Not Found from GET http://localhost:8999/api/agencies/LEI/locations?eventType=CELL",
    ) { prisonApiClient.getLocationsForTypeUnrestricted(agencyId, locationType).block() }
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
  fun `getLocationAsync do not include inactive - success`() {
    val internalLocation = internalLocation()
    prisonApiMockServer.stubGetLocation(internalLocation.locationId, internalLocation, false)
    runBlocking {
      prisonApiClient.getLocationAsync(internalLocation.locationId) isEqualTo internalLocation
    }
  }

  @Test
  fun `getLocationAsync include inactive - success`() {
    val internalLocation = internalLocation()
    prisonApiMockServer.stubGetLocation(internalLocation.locationId, internalLocation, true)
    runBlocking {
      prisonApiClient.getLocationAsync(internalLocation.locationId, true) isEqualTo internalLocation
    }
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
  fun `getExternalTransfersOnDateAsync - empty prisoner list shouldn't call API`(): Unit = runBlocking {
    val prisonCode = "MDI"
    val prisonerNumbers = emptySet<String>()
    val date = LocalDate.now()

    fun PrisonApiMockServer.verifyNoClientRequests() =
      verify(0, postRequestedFor(urlEqualTo("/api/schedules/$prisonCode/externalTransfers?date=$date")))

    prisonApiMockServer.stubGetExternalTransfersOnDate(prisonCode, prisonerNumbers, date)

    val externalTransfers = prisonApiClient.getExternalTransfersOnDateAsync(prisonCode, prisonerNumbers, date)
    assertThat(externalTransfers).hasSize(0)
    prisonApiMockServer.verifyNoClientRequests()
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
  fun `getEventLocationsAsync - success`() {
    val prisonCode = "MDI"
    val eventLocations = listOf(internalLocation(), appointmentLocation(2, prisonCode))
    prisonApiMockServer.stubGetEventLocations(prisonCode, eventLocations)
    runBlocking {
      prisonApiClient.getEventLocationsAsync(prisonCode) isEqualTo eventLocations
    }
  }

  @Test
  fun `verify overridden return type for get event locations booked`() {
    val function = PrisonApiClient::class.declaredFunctions.first { it.name == "getEventLocationsBookedAsync" }

    assertThat(function.returnType).isEqualTo(typeOf<List<LocationSummary>>())
  }

  @Test
  fun `getEventLocationsBookedAsync - success`() {
    val prisonCode = "MDI"
    val date = LocalDate.now()
    val locations = listOf(LocationSummary(locationId = 1L, description = "MDI-LOC1", userDescription = "Location 1"))
    prisonApiMockServer.stubGetEventLocationsBooked(prisonCode, date, null, locations)
    runBlocking {
      prisonApiClient.getEventLocationsBookedAsync(prisonCode, date, null) isEqualTo locations
    }
  }

  @Test
  fun `getLatestMovementForPrisoners - success when movement found`() {
    val movement = movement(prisonerNumber = "AB1235C")
    prisonApiMockServer.stubPrisonerMovements(listOf("AB1235C"), listOf(movement))
    prisonApiClient.getMovementsForPrisonersFromPrison(MOORLAND_PRISON_CODE, setOf("AB1235C")) containsExactly listOf(movement)
  }

  @Test
  fun `getLatestMovementForPrisoners - empty when no movement found`() {
    prisonApiMockServer.stubPrisonerMovements(listOf("AB1235C"), emptyList())
    prisonApiClient.getMovementsForPrisonersFromPrison(MOORLAND_PRISON_CODE, setOf("AB1235C")) containsExactly emptyList()
  }

  @Test
  fun `getLatestMovementForPrisoners - empty when no prisoners supplied`() {
    prisonApiClient.getMovementsForPrisonersFromPrison(MOORLAND_PRISON_CODE, emptySet()) containsExactly emptyList()
  }

  @Test
  fun `verify overridden return type for getMovementsForPrisonersFromPrison`() {
    val function = PrisonApiClient::class.declaredFunctions.first { it.name == "getMovementsForPrisonersFromPrison" }

    assertThat(function.returnType).isEqualTo(typeOf<List<Movement>>())
  }

  @Test
  fun `getEventLocationsForPrison - success`() {
    val eventLocations = listOf(internalLocation(), appointmentLocation(2, MOORLAND_PRISON_CODE))

    prisonApiMockServer.stubGetEventLocations(MOORLAND_PRISON_CODE, eventLocations)

    runBlocking {
      prisonApiClient.getEventLocationsForPrison(MOORLAND_PRISON_CODE) isEqualTo eventLocations.associateBy { it.locationId }
    }
  }
}
