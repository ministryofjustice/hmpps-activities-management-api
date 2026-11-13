package uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.integration

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.JsonNode
import com.github.tomakehurst.wiremock.client.WireMock
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.client.locationsinsideprison.model.ServiceUsingLocationDto
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.helpers.dpsLocation
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.LocationRemapChange
import uk.gov.justice.digital.hmpps.hmppsactivitiesmanagementapi.service.events.OutboundEvent
import java.util.UUID

@TestPropertySource(properties = ["location-remap.allowed-file=location-remap-test.csv"])
@Sql(
  "classpath:test_data/clean-all-data.sql",
  "classpath:test_data/seed-reference-data.sql",
  "classpath:test_data/seed-appointment-group-repeat-id-5.sql",
  "classpath:test_data/seed-appointment-set-id-6.sql",
  "classpath:test_data/seed-location-remap-job.sql",
)
class LocationRemapJobIntegrationTest : LocalStackTestBase() {
  private val source = UUID.fromString("44444444-1111-2222-3333-444444444444")
  private val target = UUID.fromString("22222222-2222-2222-2222-222222222222")
  private val override = UUID.fromString("66666666-6666-6666-6666-666666666666")
  private val changeLog = ListAppender<ILoggingEvent>()
  private val changeLogger = LoggerFactory.getLogger(LocationRemapChange::class.java) as Logger

  @BeforeEach
  fun stubDestination() {
    changeLog.start()
    changeLogger.addAppender(changeLog)
    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(source, dpsLocation(source, "TPR", "PARENT", "Original Laundry"))
    nomisMappingApiMockServer.stubMappingFromDpsUuid(target, 456)
    locationsInsidePrisonApiMockServer.stubLocationFromDpsUuid(
      target,
      dpsLocation(target, "TPR", "CHILD", "Laundry").copy(
        servicesUsingLocation = listOf(
          ServiceUsingLocationDto(ServiceUsingLocationDto.ServiceType.PROGRAMMES_AND_ACTIVITIES, ServiceUsingLocationDto.ServiceFamilyType.ACTIVITIES_APPOINTMENTS),
          ServiceUsingLocationDto(ServiceUsingLocationDto.ServiceType.APPOINTMENT, ServiceUsingLocationDto.ServiceFamilyType.ACTIVITIES_APPOINTMENTS),
        ),
      ),
    )
  }

  @AfterEach
  fun detachChangeLog() {
    changeLogger.detachAppender(changeLog)
    changeLog.stop()
  }

  @Test
  fun `remaps active and planned schedules future appointments and sets with outbound events`() {
    trigger(false)
    awaitSuccess(1)
    listOf(401L, 403L).forEach { id ->
      val schedule = jdbcTemplate.queryForMap("select * from activity_schedule where activity_schedule_id = ?", id)
      assertThat(schedule["dps_location_id"]).isEqualTo(target)
      assertThat(schedule["internal_location_id"]).isEqualTo(456)
      assertThat(schedule["internal_location_code"]).isEqualTo("CHILD")
      assertThat(schedule["internal_location_description"]).isEqualTo("Laundry")
      assertThat(schedule["updated_by"]).isEqualTo("activities-management-admin-1")
    }
    assertLocation("activity_schedule", "activity_schedule_id", 402, source)
    assertLocation("activity_schedule", "activity_schedule_id", 404, override)
    listOf(5L, 6L).forEach { assertLocation("appointment_series", "appointment_series_id", it, target, 456L) }
    listOf(11L, 12L, 6L).forEach { assertLocation("appointment", "appointment_id", it, target, 456L) }
    listOf(10L, 7L, 8L).forEach { assertLocation("appointment", "appointment_id", it, source) }
    assertLocation("appointment", "appointment_id", 13, override)
    assertLocation("appointment_set", "appointment_set_id", 6, target, 456L)
    assertThat(jdbcTemplate.queryForMap("select cancelled_time from appointment where appointment_id = 12")["cancelled_time"]).isNotNull()
    val categoryName = jdbcTemplate.queryForList("select description from appointment_category where code = 'OIC'", String::class.java).firstOrNull() ?: "OIC"
    assertThat(committedChanges()).hasSize(8)
    assertThat(assertCommittedChange("ACTIVITY_SCHEDULE", 401, "Laundry", "Laundry")["activityId"].asLong()).isEqualTo(401)
    assertCommittedChange("ACTIVITY_SCHEDULE", 403, "Planned", "Laundry")
    assertCommittedChange("APPOINTMENT_SERIES", 5, categoryName, "Original Laundry")
    assertCommittedChange("APPOINTMENT_SERIES", 6, "Appointment description", "Original Laundry")
    listOf(11L, 12L).forEach {
      val change = assertCommittedChange("APPOINTMENT", it, categoryName, "Original Laundry")
      assertThat(change["appointmentSeriesId"].asLong()).isEqualTo(5)
      assertThat(change["appointmentType"].asText()).isEqualTo("GROUP")
    }
    assertCommittedChange("APPOINTMENT", 6, "Appointment description", "Original Laundry")
    assertCommittedChange("APPOINTMENT_SET", 6, "Appointment description", "Original Laundry")
    validateOutboundEvents(
      ExpectedOutboundEvent(OutboundEvent.ACTIVITY_SCHEDULE_UPDATED, 401),
      ExpectedOutboundEvent(OutboundEvent.ACTIVITY_SCHEDULE_UPDATED, 403),
      ExpectedOutboundEvent(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, 22, "OIC"),
      ExpectedOutboundEvent(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, 23, "OIC"),
      ExpectedOutboundEvent(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, 24, "OIC"),
      ExpectedOutboundEvent(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, 25, "OIC"),
      ExpectedOutboundEvent(OutboundEvent.APPOINTMENT_INSTANCE_UPDATED, 6, "OIC"),
    )
    trigger(false)
    awaitSuccess(2)
    assertThat(committedChanges()).hasSize(8)
    validateNoMessagesSent()
  }

  @Test
  fun `dry run validates without changes or events`() {
    trigger(true)
    awaitSuccess(1)
    assertLocation("activity_schedule", "activity_schedule_id", 401, source)
    assertLocation("appointment", "appointment_id", 11, source)
    assertLocation("appointment_set", "appointment_set_id", 6, source)
    assertThat(committedChanges()).isEmpty()
    validateNoMessagesSent()
  }

  @Test
  fun `missing destination fails job before modifying data`() {
    locationsInsidePrisonApiMockServer.stubFor(
      WireMock.get(WireMock.urlPathEqualTo("/locations/$target")).willReturn(WireMock.aResponse().withStatus(404)),
    )
    trigger(false)
    await untilAsserted {
      assertThat(jdbcTemplate.queryForObject("select count(*) from job where job_type = 'REMAP_NON_RESIDENTIAL_LOCATIONS' and ended_at is not null and successful = false", Int::class.java)).isEqualTo(1)
    }
    assertLocation("activity_schedule", "activity_schedule_id", 401, source)
    assertLocation("appointment", "appointment_id", 11, source)
    assertThat(committedChanges()).isEmpty()
    validateNoMessagesSent()
  }

  @Test
  fun `logs CSV source names when the source location cannot be fetched`() {
    locationsInsidePrisonApiMockServer.stubFor(
      WireMock.get(WireMock.urlPathEqualTo("/locations/$source")).willReturn(WireMock.aResponse().withStatus(404)),
    )
    trigger(false)
    awaitSuccess(1)
    val appointmentChanges = committedChanges().filter { it["objectType"].asText().startsWith("APPOINTMENT") }
    assertThat(appointmentChanges).hasSize(6)
    appointmentChanges.forEach { assertThat(it["oldLocationName"].asText()).isEqualTo("Laundry, main") }
  }

  private fun committedChanges(): List<JsonNode> = changeLog.list.map { mapper.readTree(it.formattedMessage.substringAfter("Location remap committed ")) }

  private fun assertCommittedChange(type: String, id: Long, name: String, oldLocationName: String): JsonNode {
    val change = committedChanges().single { it["objectType"].asText() == type && it["objectId"].asLong() == id }
    assertThat(change["prisonCode"].asText()).isEqualTo("TPR")
    assertThat(change["name"].asText()).isEqualTo(name)
    assertThat(change["oldDpsLocationId"].asText()).isEqualTo(source.toString())
    assertThat(change["newDpsLocationId"].asText()).isEqualTo(target.toString())
    assertThat(change["oldLocationName"].asText()).isEqualTo(oldLocationName)
    assertThat(change["newLocationName"].asText()).isEqualTo("Laundry")
    assertThat(change["oldNomisLocationId"].asLong()).isEqualTo(123)
    assertThat(change["newNomisLocationId"].asLong()).isEqualTo(456)
    return change
  }

  private fun trigger(dryRun: Boolean) {
    webTestClient.post()
      .uri("/job/remap-non-residential-locations?file=location-remap-test.csv&prisons=TPR&dryRun=$dryRun")
      .exchange().expectStatus().isAccepted
  }

  private fun awaitSuccess(count: Int) {
    await untilAsserted {
      assertThat(jdbcTemplate.queryForObject("select count(*) from job where job_type = 'REMAP_NON_RESIDENTIAL_LOCATIONS' and successful = true", Int::class.java)).isEqualTo(count)
    }
  }

  private fun assertLocation(table: String, idColumn: String, id: Long, expected: UUID, nomisId: Long? = null) {
    val record = jdbcTemplate.queryForMap("select dps_location_id, internal_location_id from $table where $idColumn = ?", id)
    assertThat(record["dps_location_id"]).isEqualTo(expected)
    if (nomisId != null) assertThat((record["internal_location_id"] as Number).toLong()).isEqualTo(nomisId)
  }
}
